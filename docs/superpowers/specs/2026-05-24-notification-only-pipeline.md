# Notification-Only Transaction Pipeline — Design Spec
**Date:** 2026-05-24
**Owner:** MR FIDRIYANTO
**Status:** Approved

---

## Problem

The current pipeline has a race condition: a push notification fires before the corresponding email arrives in Gmail, so `fetchLatestBankEmail()` returns the previous email. This causes the wrong amount to be saved and creates duplicate records. The root cause is that `triggerAmount` extracted from the notification is never validated against the email amount.

Additionally, `processed_refs` is unused in practice because BillPayment transactions (BTS, etc.) have no reference number in their emails, leaving deduplication completely broken.

---

## Goal

Remove the Gmail fetch step entirely. Parse all transaction data directly from the push notification. The notification is the single source of truth.

---

## What Bangkok Bank Notifications Contain

| Notification Extra | Content | Used For |
|---|---|---|
| `EXTRA_TITLE` | Channel keyword: "ชำระบิล / Bill Payment", "โอนเงิน / Transfer", "e-Wallet" etc. | Detect `channel` |
| `EXTRA_TEXT` | Merchant name + amount: e.g. `"BTS TIM TVM 65.00THB"` | Extract `merchant` + `amount` |
| System timestamp | Notification arrival time | Derive `dateIso` |

---

## Architecture Changes

### Deleted
- `EmailFetcher` — removed entirely
- `EmailFetchWorker` — removed entirely (replaced by direct processing)
- `EmailParser` — removed entirely
- `gmail.readonly` OAuth scope — removed from auth flow

### New
- `NotificationParser` — parses `EXTRA_TITLE` + `EXTRA_TEXT` into `ParsedTransaction`. Mirrors `EmailParser`'s channel-detection logic adapted for notification strings.

### Modified
- `BankNotificationService` — reads both `EXTRA_TITLE` and `EXTRA_TEXT`; calls `NotificationParser`; passes `ParsedTransaction` directly to `TransactionRepository` via a coroutine scope (no WorkManager needed)
- `TransactionRepository.processNewNotification` — accepts `ParsedTransaction` instead of `Double`; no network call; deduplication via composite key
- `processed_refs` — key changes from `referenceNo` (often empty) to a composite of `merchant + amount + dateIso`; `ProcessedRefEntity` updated accordingly

---

## NotificationParser

```
Input:  title: String, text: String, timestampMs: Long
Output: ParsedTransaction?
```

**Channel detection from title:**

| Title contains (case-insensitive) | `channel` |
|---|---|
| "bill payment" or "ชำระบิล" | `BillPayment` |
| "e-wallet" or "ewallet" | `eWallet` |
| "promptpay" or "พร้อมเพย์" | `PromptPay` |
| "transfer" or "โอนเงิน" | `BankTransfer` |
| No match | `Unknown` — transaction is skipped |

**Field extraction from text:**

- Amount: existing regex `(\d[\d,]*(?:\.\d{1,2})?)THB` (already in `BankNotificationService`)
- Merchant: everything in `EXTRA_TEXT` before the amount+THB pattern, trimmed
- Date: `LocalDate` from `Instant.ofEpochMilli(timestampMs)` in Asia/Bangkok timezone
- `referenceNo`: always `""` (not available in notification)

**Returns null** if amount cannot be extracted or channel is Unknown (notification is not a payment).

---

## Deduplication

Replace `referenceNo` string key with composite key: `"$merchant|$amount|$dateIso"`.

- Same merchant, same amount, same calendar day = duplicate, silently dropped
- Legitimate double-spend (e.g. two BTS rides same day) is a known limitation; user must add the second manually

`ProcessedRefEntity` primary key changes from `referenceNo TEXT` to `compositeKey TEXT`. The table is renamed or migrated via Room migration.

---

## Transaction Flow (After)

```
Bangkok Bank push notification arrives
  → BankNotificationService.onNotificationPosted
  → NotificationParser.parse(title, text, timestamp) → ParsedTransaction?
  → null: return (not a payment notification)
  → TransactionRepository.processNewNotification(parsed)
      → compositeKey = merchant|amount|dateIso
      → processed_refs: already seen? drop silently
      → processed_refs: insert compositeKey
      → CategoryResolver.resolve(parsed, ...)
      → TransactionEntity inserted (status = PENDING_EDIT)
  → ReviewNotificationManager.showReviewNotification(id)
```

No network calls in the hot path. Gmail API dependency removed.

---

## Room Migration

Add `Migration(N, N+1)`:
- Drop `processed_refs` table
- Recreate with `compositeKey TEXT NOT NULL PRIMARY KEY`

---

## OAuth Scope Change

Remove `https://www.googleapis.com/auth/gmail.readonly` from the Google Sign-In scope list. Users who previously granted this scope will not be affected (Android does not revoke already-granted scopes), but new sign-ins will not request it.

---

## Testing

- `NotificationParserTest` — unit tests for each channel type, merchant extraction, amount regex edge cases, null cases
- `TransactionRepositoryTest` — update `processNewNotification` tests to pass `ParsedTransaction` directly
- Delete `EmailParserTest` and `EmailFetcherTest`
- Existing `CategoryResolverTest` unchanged

---

## Out of Scope

- Mandiri email parsing (already manual-only)
- Handling notifications from other banks
- Reference number recovery (not available in notifications)
