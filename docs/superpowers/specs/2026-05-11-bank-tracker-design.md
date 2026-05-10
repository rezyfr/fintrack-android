# BankTracker Android — Design Spec
**Date:** 2026-05-11  
**Owner:** MR FIDRIYANTO  
**Status:** Approved

---

## Overview

A personal Android app that listens for Bangkok Bank push notifications, fetches the corresponding email for full transaction details, categorizes the transaction via Claude AI or rule-based matching, prompts the user to review within 3 minutes, then auto-syncs to an existing Google Sheet ("Money Tracker 2026").

**Not** a Play Store release. Single-user. No backend server.

---

## Goals

- Auto-capture Bangkok Bank (THB) expense transactions with zero manual effort
- Support manual entry for IDR expenses, IDR income, and THB income
- Sync accurately to the existing Money Tracker 2026 Google Sheet without breaking SUMIFS formulas
- Minimize Claude API calls by using rule-based categorization for known merchants

**Out of scope (for now):**
- Mandiri email parsing (manual IDR entry only)
- Income auto-capture (income entered manually)
- Monthly/yearly overview charts (planned future tab)
- Play Store distribution

---

## Tech Stack

| Layer | Choice |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Background service | `NotificationListenerService` |
| Local DB | Room (offline queue + dedup log) |
| Background work | WorkManager (retry-on-connectivity) |
| HTTP | Retrofit + OkHttp |
| Auth | Google Sign-In (single OAuth flow: Gmail + Sheets scopes) |
| Secrets | `EncryptedSharedPreferences` (OAuth tokens, Claude API key) |

---

## Architecture — Fully On-Device

No backend. All API calls made directly from the Android app.

### Components

| Component | Responsibility |
|---|---|
| `NotificationListenerService` | Detects Bangkok Bank push notifications; extracts amount + timestamp as trigger |
| `EmailFetcher` | Polls Gmail API ~5s after trigger; fetches latest Bangkok Bank email |
| `EmailParser` | Parses HTML table email into key→value map; detects format; resolves merchant fields |
| `CategoryResolver` | 3-tier: rule match → PromptPay heuristic → Claude Haiku fallback |
| `ReviewNotificationManager` | Posts editable Android notification with 3-min countdown; cancels on user action |
| `SheetsSyncer` | Appends row to correct Sheets tab via Sheets API v4 (`USER_ENTERED` mode) |
| `OfflineQueue` | Room DB table; WorkManager retries failed syncs when connectivity returns |
| `DuplicateGuard` | Stores email reference numbers in Room; drops duplicate triggers silently |

### Transaction Pipeline

```
Bangkok Bank push notification (trigger)
  → Extract: amount + timestamp from notification text
  → Wait 5s
  → Gmail API: fetch latest Bangkok Bank email (subject/sender filter)
  → EmailParser: detect format, extract merchant + amount + date + fee
  → DuplicateGuard: check reference number — drop if seen before
  → CategoryResolver:
      1. Fuzzy rule match against known merchants → category (no API call)
      2. PromptPay + amount ∈ {30000, 50000} → Transfer Out (no API call)
      3. Claude Haiku → category + clean description (cached by merchant key)
  → ReviewNotificationManager: post notification with parsed data
  → 3-minute timer:
      User edits → sync immediately with user's values
      No action  → sync with resolved values
  → SheetsSyncer: append to Expenses tab (USER_ENTERED mode)
  → DuplicateGuard: store reference number
```

---

## Bangkok Bank Email Formats

The parser extracts all table fields into a `Map<String, String>` then detects format by key presence.

| Format | Detected By | Merchant Field | Typical Category |
|---|---|---|---|
| Bill / Service payment | `Service name / Payee name` present | `Service name / Payee name` | Rule match or Claude |
| E-wallet transfer | `e-wallet provider name` present | `e-wallet provider name` + owner | Claude |
| PromptPay person transfer | `Receiving method` contains "PromptPay" | `Account name` | Amount heuristic or Claude |
| Bank-to-bank transfer | `Bank` field present in To section | `Account name` + `Bank` | Amount heuristic or Claude |

**Merchant normalization:** Strip legal suffixes (`CO., LTD.`, `PLC`, `PCL`), uppercase, remove punctuation before rule matching.

**Date parsing:** Email format `"10 May 2026 at 17:52:34 (Thailand time)"` → stored as `"10/5/2026"` (D/M/YYYY text string, no leading zeros) for Expenses tab.

**Fee handling:** If `Fee (Baht) > 0`, add to amount. Note fee in Item description.

---

## CategoryResolver — 3-Tier Logic

### Tier 1: Rule-based fuzzy match
Built-in merchant→category map (seeded from `governance/sheet-rules.md`). Normalized merchant matched against known list. No API call.

### Tier 2: PromptPay heuristic
If channel = PromptPay AND amount ≥ 25,000 THB → category = `Transfer Out` (covers the monthly 30k–50k Indonesia transfer). Flagged with ⚠ in review notification for user confirmation. Threshold configurable in Settings.

### Tier 3: Claude Haiku fallback
**Input:** merchant name + amount + channel + full category list  
**Output:** category (exact match from list) + clean item description  
**Caching:** Response cached in Room by normalized merchant key — same merchant never calls API twice  
**Failure:** Defaults to `Other`; does not retry (avoids duplicate cost)

---

## Google Sheets Integration

**Spreadsheet ID:** `1OJqLIPFWjJPje8HabLVyMp_AGvsCi9nWM2nHqdzv9-w`

### Write Rules (must follow exactly)

| Tab | Date Format | Amount Format | API Mode |
|---|---|---|---|
| `Expenses` | `D/M/YYYY` text string (e.g. `"10/5/2026"`) | Numeric string, no trailing `.0` | `USER_ENTERED` |
| `IDR Expenses` | `D/M/YYYY` text string | Numeric string | `USER_ENTERED` |
| `Income` | `DD/MM/YYYY` real date (e.g. `"26/01/2026"`) | Numeric string | `USER_ENTERED` |
| `IDR Income` | `DD/MM/YYYY` real date | Numeric string | `USER_ENTERED` |

**Never use `RAW` mode** — breaks SUMIFS formulas in Monthly Overview.

### Valid Categories (case-sensitive, must match exactly)
`Bills` · `Subscriptions` · `Entertainment` · `Food & Drink` · `Groceries` · `Health & Wellbeing` · `Other` · `Shopping` · `Transport` · `Travel` · `Business` · `Gifts` · `Transfer Out`

---

## OAuth & Authentication

Single Google Sign-In flow grants both required scopes:
- `https://www.googleapis.com/auth/gmail.readonly`
- `https://www.googleapis.com/auth/spreadsheets`

Tokens stored in `EncryptedSharedPreferences`. Token refresh handled automatically via `AccountManager`. On refresh failure → in-app banner prompts re-auth; pending transactions held in queue.

Claude API key entered manually in Settings screen, stored in `EncryptedSharedPreferences`.

---

## UI Design

### Theme
- **Style:** OLED Dark Mode (Material 3 dark color scheme)
- **Font:** Plus Jakarta Sans
- **Icons:** Material Symbols (outlined, consistent 24dp)

### Color Tokens
| Token | Value | Usage |
|---|---|---|
| Background | `#0F172A` | Screen background |
| Surface | `#1E293B` | Cards, bottom sheet |
| Primary | `#1E40AF` | Pending border, active nav, toggles |
| Secondary | `#3B82F6` | Countdown text, links |
| Accent | `#059669` | Synced badge, CTA button |
| Amount (expense) | `#F87171` | Expense amounts |
| Warning | `#F59E0B` | Pending sync badge |
| Destructive | `#DC2626` | Sync failed badge, delete |
| Muted text | `#64748B` | Secondary labels, timestamps |

### Navigation
3-tab `NavigationBar` (Material 3):
1. **Feed** — auto-captured transaction list
2. **Add** — manual entry bottom sheet
3. **Settings** — Google auth, Claude API key, service status

### Feed Screen (Option A — Card List)
Each transaction is a `Card` with:
- Merchant name (title) + category + time (subtitle)
- Amount (right-aligned, red for expenses)
- State badge: `Pending (countdown)` / `Synced` / `Pending Sync` / `Sync Failed`

**Card border color by state:**
- Pending edit: `#1E40AF` (blue)
- Synced: no border
- Pending sync (offline): `#F59E0B` (amber)
- Sync failed: `#DC2626` (red, tap to retry)

### Manual Entry Bottom Sheet
Fields: Account (THB / IDR toggle) → Type (Expense / Income toggle) → Amount → Description → Category (dropdown, 13 options) → Date (defaults today)  
Primary CTA: **Sync to Sheets** (`#059669`)

### Settings Screen
- Google account display + Sign Out
- Claude API key field (masked, show/hide toggle)
- Service status indicator: `● Listening` (green) / `● Inactive` (red) with deep link to Android notification access settings
- Manual sync button for pending queue

---

## Error Handling

| Scenario | Behavior |
|---|---|
| Email not found after 3 retries | Notification: "Tap to enter manually" — opens entry sheet pre-filled with amount |
| Sheets API fails | Save to Room offline queue; show ⚠ badge; WorkManager retries on connectivity |
| Claude API fails / timeout | Default to `Other`; no retry; user can fix in review notification |
| OAuth token refresh fails | In-app banner; queue held until re-auth |
| `NotificationListenerService` killed | Settings shows `● Inactive`; deep link to re-enable |
| Duplicate notification trigger | Reference number checked in Room; duplicate silently dropped |

---

## Future Considerations (not in scope now)

- Monthly/yearly overview tab (4th nav item) with spending by category charts
- Gmail API polling for Mandiri email parsing (IDR auto-capture)
- Bangkok Bank income notification detection (if format is discovered)
- Widget showing current month spend

---

## Key Governance Rules (from `governance/sheet-rules.md`)

- Always `USER_ENTERED` mode — never `RAW`
- `D/M/YYYY` for Expenses (text, no leading zeros) — breaks SUMIFS if wrong
- Categories must match Monthly Overview column headers exactly (case-sensitive)
- Test with 5 rows before batch appending
- Amounts as numeric strings without trailing `.0`
