# Dashboard Screen — Design Spec

**Date:** 2026-05-16

## Goal

Add a Dashboard tab to FinTrack that shows spending by category and income vs. expenses balance, broken down by period, for both THB and IDR currencies. Data is sourced from the Google Sheets "Monthly Overview" and "Monthly Overview IDR" tabs — the canonical source of truth — and cached locally in Room for offline access.

## Navigation

A fourth tab is added to the bottom nav between Add and Settings:

```
Feed | Add | Dashboard | Settings
```

## Screen Layout

Scrollable `Column` with 16dp horizontal padding, matching existing screens.

```
[Dashboard]                          ← screen title

[This Month] [Last Month] [3 Months] ← FilterChip period selector

── THB ────────────────────────────

┌─────────────────────────────────┐
│ Income      Expenses      Net   │  ← balance card
│ ฿45,200     ฿32,100   +฿13,100 │
└─────────────────────────────────┘

┌─────────────────────────────────┐
│ Spending Breakdown   ฿32,100    │  ← category card
│                                 │
│ Food & Drink         ฿12,400   │
│ ████████████░░░░░░░       39%  │
│                                 │
│ Transport             ฿6,800   │
│ ████████░░░░░░░░░░░       21%  │
│ ...                             │
└─────────────────────────────────┘

── IDR ────────────────────────────
[same two cards with Rp amounts]
```

### Balance Card
- Three equally-spaced columns: Income, Expenses, Net
- Net is green (#4CAF50) when positive, red (Destructive) when negative
- Uses white for Income and Expenses labels

### Category Card
- Header row: "Spending Breakdown" label + total amount right-aligned
- Each category row: name, amount right-aligned, percentage muted
- Progress bar below each row: filled green (#4CAF50), unfilled dark gray
- Bar width = `amount / totalExpenses`
- Categories with ฿0 / Rp0 are omitted
- Rows sorted by amount descending

### Period Selector
- `FilterChip` row: This Month, Last Month, Last 3 Months
- "Last 3 Months" sums the three most recent complete months client-side

### Currency Section Headers
- `Text("── THB ──────────────────────")` in muted color
- Same for IDR

## Data Source

**Primary:** Google Sheets tabs read via Sheets API:
- `Monthly Overview!A1:Q17` — THB data
- `Monthly Overview IDR!A1:Q17` — IDR data

Sheet structure (row 1 = headers, rows 2–13 = Jan–Dec, row 15 = Total, row 17 = Monthly Budget):

| Col | Field |
|-----|-------|
| A | Month ("January 2026") |
| B | Bills |
| C | Subscriptions |
| D | Entertainment |
| E | Food & Drink |
| F | Groceries |
| G | Health & Wellbeing |
| H | Other |
| I | Shopping |
| J | Transport |
| K | Travel |
| L | Business |
| M | Gifts |
| N | Total Expenditure |
| O | Income |
| P | Gross Savings |

**Cache:** Room DB. Two new entities:
- `MonthlyOverviewEntity` — one row per month per currency (24 rows max)
- `MonthlyBudgetEntity` — one row per currency, stores the Monthly Budget row

## New Components

### `sheets/MonthlyOverviewFetcher.kt`
- Fetches both tabs in two parallel calls
- Reuses `GoogleAuthManager` (access token) and `OkHttpClient`
- Parses the range response into `List<MonthlyOverviewEntity>`
- Returns `Result<Unit>` — caller handles failure

### `data/db/MonthlyOverviewDao.kt`
```kotlin
fun observeByMonths(months: List<String>, currency: String): Flow<List<MonthlyOverviewEntity>>
suspend fun upsertAll(rows: List<MonthlyOverviewEntity>)
suspend fun upsertBudget(budget: MonthlyBudgetEntity)
fun observeBudget(currency: String): Flow<MonthlyBudgetEntity?>
```

### `data/repository/DashboardRepository.kt`
- `observeDashboard(period: Period): Flow<DashboardState>` — joins cache data into `DashboardState`
- `suspend fun refresh(): Result<Unit>` — fetches from Sheets, upserts to cache

### `ui/dashboard/DashboardViewModel.kt`
- `val period: StateFlow<Period>` — driven by chip selection
- `val state: StateFlow<DashboardUiState>` — flatMapLatest on period
- `fun selectPeriod(period: Period)`
- `fun refresh()` — triggers `DashboardRepository.refresh()`

### `ui/dashboard/DashboardScreen.kt`
- Observes `DashboardUiState`
- `PullToRefreshBox` wrapping the scroll column
- Delegates to `BalanceCard` and `CategoryBreakdownCard` composables

## State Model

```kotlin
sealed class DashboardUiState {
    object NotSignedIn : DashboardUiState()
    object LoadingNoCache : DashboardUiState()
    data class Loaded(
        val period: Period,
        val thb: CurrencySummary,
        val idr: CurrencySummary,
        val isRefreshing: Boolean,
        val lastUpdated: String?,   // "2 mins ago" — null if never synced
        val refreshError: Boolean
    ) : DashboardUiState()
}

data class CurrencySummary(
    val totalIncome: Double,
    val totalExpenses: Double,
    val net: Double,
    val categoryBreakdown: List<CategoryRow>
)

data class CategoryRow(
    val category: String,
    val amount: Double,
    val percentage: Float
)

enum class Period { THIS_MONTH, LAST_MONTH, LAST_3_MONTHS }
```

## Error & Empty States

| Condition | Behaviour |
|-----------|-----------|
| Not signed in | Centered card: "Sign in with Google to load your dashboard" + button to Settings |
| First load, no cache | Skeleton shimmer on both currency sections |
| Refresh failed, cache exists | Show cached data + "Last updated X mins ago" subtitle |
| Refresh failed, no cache | Error card: "Couldn't load data. Pull to refresh." |
| No data for period | Balance card shows ฿0 / Rp0, category list: "No spending recorded" |
| Net is negative | Net value shown in red (Destructive color) |

## Out of Scope

- Budget vs. actual comparison (Monthly Budget row is cached and ready, but not displayed in this spec)
- IDR/THB unified conversion view
- Editing data from the Dashboard
- Yearly or custom date range views
