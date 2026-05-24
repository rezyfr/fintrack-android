# Supabase Backend Migration — Design Spec
**Date:** 2026-05-25
**Owner:** MR FIDRIYANTO
**Status:** Approved

---

## Overview

Replace Google Sheets as the cloud data store with Supabase (PostgreSQL). The Android app's `SheetsSyncer` interface is the seam point — only the implementation swaps. Google OAuth is removed entirely. Historical Sheets data is migrated via a one-time Python script.

This is Sub-project 1 of 3 (Backend). Sub-projects 2 (Web app) and 3 (Insights) build on top.

---

## Goals

- Supabase as the single source of truth for transactions and budgets
- Web app and insights become possible (Sub-projects 2 and 3)
- Remove Google auth dependency from the Android app
- Migrate all historical Sheets data into Supabase
- Offline queue and retry behavior unchanged

---

## Supabase Schema

### `transactions`

| Column | Type | Notes |
|---|---|---|
| `id` | `BIGSERIAL PRIMARY KEY` | Supabase-generated |
| `merchant` | `TEXT NOT NULL` | Raw merchant name |
| `item` | `TEXT NOT NULL` | Cleaned description (from CategoryResolver) |
| `amount` | `NUMERIC NOT NULL` | |
| `category` | `TEXT NOT NULL` | |
| `date` | `DATE NOT NULL` | Parsed from `dateIso` |
| `channel` | `TEXT NOT NULL` | BillPayment, eWallet, PromptPay, BankTransfer, Manual, Unknown |
| `tab` | `TEXT NOT NULL` | EXPENSES, IDR_EXPENSES, INCOME, IDR_INCOME |
| `note` | `TEXT` | Nullable — user-added context, not auto-populated |
| `created_at` | `TIMESTAMPTZ DEFAULT NOW()` | |

### `budgets`

| Column | Type | Notes |
|---|---|---|
| `currency` | `TEXT PRIMARY KEY` | `"THB"` or `"IDR"` |
| `bills` | `NUMERIC NOT NULL` | |
| `subscriptions` | `NUMERIC NOT NULL` | |
| `entertainment` | `NUMERIC NOT NULL` | |
| `food_drink` | `NUMERIC NOT NULL` | |
| `groceries` | `NUMERIC NOT NULL` | |
| `health_wellbeing` | `NUMERIC NOT NULL` | |
| `other` | `NUMERIC NOT NULL` | |
| `shopping` | `NUMERIC NOT NULL` | |
| `transport` | `NUMERIC NOT NULL` | |
| `travel` | `NUMERIC NOT NULL` | |
| `business` | `NUMERIC NOT NULL` | |
| `gifts` | `NUMERIC NOT NULL` | |

Only two rows ever (THB and IDR). Mirrors `MonthlyBudgetEntity` in Room exactly.

---

## Architecture

```
Current:  Room → SheetsSyncerImpl → Google Sheets API (OAuth token)
After:    Room → SupabaseSyncerImpl → Supabase REST API (static API key)
```

The `SheetsSyncer` interface signature (`suspend fun sync(row: SheetsRow): Result<Unit>`) is unchanged. `TransactionRepository` is untouched except for the `SheetsRow` construction.

---

## Android Changes

### Files to Create

| File | Responsibility |
|---|---|
| `sheets/SupabaseSyncerImpl.kt` | OkHttp POST to `https://[url]/rest/v1/transactions` with `apikey` + `Prefer: return=minimal` headers |
| `data/repository/BudgetRepository.kt` | Read/write `budgets` table via Supabase REST; replaces budget reads from Room |

### Files to Modify

| File | Change |
|---|---|
| `data/model/SheetsRow.kt` | Add `merchant: String`, `channel: String`, `note: String? = null` |
| `data/repository/TransactionRepository.kt` | Populate new `SheetsRow` fields from `TransactionEntity` in `syncTransaction` |
| `di/SheetsModule.kt` | Bind `SupabaseSyncerImpl` instead of `SheetsSyncerImpl` |
| `app/build.gradle.kts` | Add `SUPABASE_URL` + `SUPABASE_ANON_KEY` BuildConfig fields from `local.properties` |
| `ui/settings/SettingsViewModel.kt` | Remove `GoogleAuthManager` injection; remove `isSignedIn`, `accountEmail`, `consentIntent`, `getSignInIntent`, `signOut` |
| `ui/settings/SettingsScreen.kt` | Remove Google account section (email display + Sign Out button) |

### Files to Delete

| File | Reason |
|---|---|
| `sheets/SheetsSyncerImpl.kt` | Replaced by `SupabaseSyncerImpl` |
| `auth/GoogleAuthManager.kt` | No longer needed |
| `auth/GoogleAuthManagerImpl.kt` | No longer needed |
| `di/AuthModule.kt` | No longer needed |

### BuildConfig

Add to `local.properties` (not committed to git):
```
SUPABASE_URL=https://[project-ref].supabase.co
SUPABASE_ANON_KEY=[anon-key]
```

Add to `app/build.gradle.kts` alongside existing `SPREADSHEET_ID` field:
```kotlin
buildConfigField("String", "SUPABASE_URL", "\"${localProps.getProperty("SUPABASE_URL", "")}\"")
buildConfigField("String", "SUPABASE_ANON_KEY", "\"${localProps.getProperty("SUPABASE_ANON_KEY", "")}\"")
```

Remove `SPREADSHEET_ID` BuildConfig field.

---

## SupabaseSyncerImpl

Posts to `[SUPABASE_URL]/rest/v1/transactions` with:
- Header `apikey: [SUPABASE_ANON_KEY]`
- Header `Content-Type: application/json`
- Header `Prefer: return=minimal`
- Body: JSON object with all `SheetsRow` fields mapped to column names

On HTTP 2xx → `Result.success(Unit)`. On any other response or exception → `Result.failure(...)`. No retry logic here — `SyncWorker` handles retries.

Budget sync (`BudgetRepository`) uses the same OkHttp pattern: GET/UPSERT to `[SUPABASE_URL]/rest/v1/budgets` with `on_conflict=currency` for upserts.

---

## SheetsRow Extension

```kotlin
data class SheetsRow(
    val tab: SheetTab,
    val date: LocalDate,
    val merchant: String,      // new
    val item: String,
    val amount: Double,
    val category: String,
    val channel: String,       // new
    val note: String? = null   // new
)
```

`TransactionRepository.syncTransaction` already reads the full `TransactionEntity` — it populates `merchant` and `channel` from `entity.merchant` and `entity.channel`.

---

## Error Handling

Identical to current behavior:
- Sync failure → Room status `SYNC_FAILED` → `SyncWorker` retries on connectivity
- API key invalid → every sync fails HTTP 401 → all transactions show `SYNC_FAILED` badge in Feed
- No token refresh, no consent flow

---

## Data Migration

### Tool
One-time Python script (`scripts/migrate_sheets_to_supabase.py`). Requires:
- Google service account JSON key with Sheets read access
- `SUPABASE_URL` and `SUPABASE_ANON_KEY`

### Process
1. For each of the 4 tabs (Expenses, IDR Expenses, Income, IDR Income):
   - Read all rows via Sheets API
   - Parse date from `D/M/YYYY` or `DD/MM/YYYY` format
   - Map tab name → `tab` enum string
   - Set `merchant = item`, `channel = "Unknown"` (not stored in Sheets)
2. Batch POST to `[SUPABASE_URL]/rest/v1/transactions`
3. Log row count and any failures

### Room backfill
After `SupabaseSyncerImpl` is live, trigger "Retry Pending Syncs" from Settings to push the 25 on-device Room transactions into Supabase with full `merchant` and `channel` data.

---

## Google Auth Removal

`GoogleAuthManager` is only used by:
- `SheetsSyncerImpl` (deleted)
- `SettingsViewModel` (sign-in/out, email display — removed)

After removal, `GoogleSignIn` and `GoogleAccountCredential` dependencies can be removed from `build.gradle.kts` if no other library pulls them in.

---

## Out of Scope

- Web app UI (Sub-project 2)
- Insights/analytics (Sub-project 3)
- Row Level Security on Supabase (single-user personal app, anon key is sufficient)
- Real-time subscriptions (Sub-project 2+)
- `MonthlyOverviewFetcher` / `MonthlyOverviewEntity` — currently fetches from Sheets; will be replaced by SQL aggregation in Sub-project 3. Left as-is for now.
