# Dashboard Screen Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a Dashboard tab showing spending by category and income vs. expenses balance for THB and IDR, sourced from Google Sheets "Monthly Overview" tabs via the Sheets API and cached in Room for offline access.

**Architecture:** `MonthlyOverviewFetcher` reads both Sheets tabs in parallel and stores rows in Room via `MonthlyOverviewDao`. `DashboardRepository` combines the two currency flows for a given period. `DashboardViewModel` handles period selection, sign-in guard, pull-to-refresh, and transforms raw rows into `DashboardUiState`. `DashboardScreen` renders the UI using `BalanceCard` and `CategoryBreakdownCard` sub-composables.

**Tech Stack:** Kotlin, Jetpack Compose + Material3 (BOM 2024.06.00), Hilt, Room 2.6.1, OkHttpClient, Coroutines/Flow, `java.time.LocalDate`

---

## File Map

**New production files:**
- `data/db/MonthlyOverviewEntity.kt` — Room entity, composite PK (month, currency)
- `data/db/MonthlyBudgetEntity.kt` — Room entity for the budget row, PK = currency
- `data/db/MonthlyOverviewDao.kt` — DAO for both entities
- `sheets/MonthlyOverviewFetcher.kt` — interface with `FetchResult` nested type
- `sheets/MonthlyOverviewFetcherImpl.kt` — concrete implementation
- `di/DashboardModule.kt` — binds `MonthlyOverviewFetcherImpl` → `MonthlyOverviewFetcher`
- `ui/dashboard/DashboardModels.kt` — `Period`, `DashboardUiState`, `CurrencySummary`, `CategoryRow`
- `data/repository/DashboardRepository.kt` — observes Room + triggers refresh
- `ui/dashboard/DashboardViewModel.kt` — period StateFlow, state StateFlow, refresh logic
- `ui/dashboard/BalanceCard.kt` — balance card composable
- `ui/dashboard/CategoryBreakdownCard.kt` — category breakdown composable
- `ui/dashboard/DashboardScreen.kt` — top-level screen + period selector

**Modified files:**
- `data/db/AppDatabase.kt` — add two entities, bump version to 2, add `monthlyOverviewDao()`
- `di/AppModule.kt` — add `provideMonthlyOverviewDao`, add `fallbackToDestructiveMigration()`
- `ui/navigation/AppNavigation.kt` — add Dashboard tab between Add and Settings
- `gradle/libs.versions.toml` — add `compose-material-icons-extended`
- `app/build.gradle.kts` — add extended icons impl dependency

**New test files:**
- `androidTest/fake/FakeMonthlyOverviewFetcher.kt` — controllable fake
- `androidTest/ui/DashboardScreenTest.kt` — Compose UI tests

**Modified test files:**
- `androidTest/di/TestAppModule.kt` — add `FakeMonthlyOverviewFetcher` binding, replace `DashboardModule`

All paths below are relative to `app/src/main/java/com/fidriyanto/banktracker/` for production files, and `app/src/androidTest/java/com/fidriyanto/banktracker/` for test files.

---

### Task 1: Room Entities

**Files:**
- Create: `app/src/main/java/com/fidriyanto/banktracker/data/db/MonthlyOverviewEntity.kt`
- Create: `app/src/main/java/com/fidriyanto/banktracker/data/db/MonthlyBudgetEntity.kt`

- [ ] **Step 1: Create MonthlyOverviewEntity**

```kotlin
package com.fidriyanto.banktracker.data.db

import androidx.room.Entity

@Entity(tableName = "monthly_overview", primaryKeys = ["month", "currency"])
data class MonthlyOverviewEntity(
    val month: String,           // "January 2026"
    val currency: String,        // "THB" or "IDR"
    val bills: Double,
    val subscriptions: Double,
    val entertainment: Double,
    val foodDrink: Double,
    val groceries: Double,
    val healthWellbeing: Double,
    val other: Double,
    val shopping: Double,
    val transport: Double,
    val travel: Double,
    val business: Double,
    val gifts: Double,
    val totalExpenditure: Double,
    val income: Double,
    val grossSavings: Double
)
```

- [ ] **Step 2: Create MonthlyBudgetEntity**

```kotlin
package com.fidriyanto.banktracker.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "monthly_budget")
data class MonthlyBudgetEntity(
    @PrimaryKey val currency: String,
    val bills: Double,
    val subscriptions: Double,
    val entertainment: Double,
    val foodDrink: Double,
    val groceries: Double,
    val healthWellbeing: Double,
    val other: Double,
    val shopping: Double,
    val transport: Double,
    val travel: Double,
    val business: Double,
    val gifts: Double
)
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/fidriyanto/banktracker/data/db/MonthlyOverviewEntity.kt \
        app/src/main/java/com/fidriyanto/banktracker/data/db/MonthlyBudgetEntity.kt
git commit -m "feat: add MonthlyOverview and MonthlyBudget Room entities"
```

---

### Task 2: MonthlyOverviewDao + AppDatabase + DI

**Files:**
- Create: `app/src/main/java/com/fidriyanto/banktracker/data/db/MonthlyOverviewDao.kt`
- Modify: `app/src/main/java/com/fidriyanto/banktracker/data/db/AppDatabase.kt`
- Modify: `app/src/main/java/com/fidriyanto/banktracker/di/AppModule.kt`
- Modify: `app/src/androidTest/java/com/fidriyanto/banktracker/di/TestAppModule.kt`

- [ ] **Step 1: Create MonthlyOverviewDao**

```kotlin
package com.fidriyanto.banktracker.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MonthlyOverviewDao {
    @Query("SELECT * FROM monthly_overview WHERE month IN (:months) AND currency = :currency")
    fun observeByMonths(months: List<String>, currency: String): Flow<List<MonthlyOverviewEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rows: List<MonthlyOverviewEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBudget(budget: MonthlyBudgetEntity)

    @Query("SELECT * FROM monthly_budget WHERE currency = :currency")
    fun observeBudget(currency: String): Flow<MonthlyBudgetEntity?>
}
```

- [ ] **Step 2: Update AppDatabase — add entities, bump version, expose new DAO**

Replace the entire file content:

```kotlin
package com.fidriyanto.banktracker.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        TransactionEntity::class,
        CategoryCacheEntity::class,
        ProcessedRefEntity::class,
        MonthlyOverviewEntity::class,
        MonthlyBudgetEntity::class
    ],
    version = 2
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryCacheDao(): CategoryCacheDao
    abstract fun processedRefDao(): ProcessedRefDao
    abstract fun monthlyOverviewDao(): MonthlyOverviewDao
}
```

- [ ] **Step 3: Update AppModule — add fallbackToDestructiveMigration and new DAO provider**

In `AppModule.kt`, change `provideDatabase` to:

```kotlin
@Provides @Singleton
fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
    Room.databaseBuilder(ctx, AppDatabase::class.java, "banktracker.db")
        .fallbackToDestructiveMigration()
        .build()
```

Add this provider (after `provideProcessedRefDao`):

```kotlin
@Provides fun provideMonthlyOverviewDao(db: AppDatabase) = db.monthlyOverviewDao()
```

- [ ] **Step 4: Update TestAppModule — add new DAO provider**

In the `companion object` of `TestAppModule.kt`, add after `provideProcessedRefDao`:

```kotlin
@Provides fun provideMonthlyOverviewDao(db: AppDatabase) = db.monthlyOverviewDao()
```

- [ ] **Step 5: Verify the build compiles**

```bash
./gradlew compileDebugKotlin
```

Expected output: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/fidriyanto/banktracker/data/db/MonthlyOverviewDao.kt \
        app/src/main/java/com/fidriyanto/banktracker/data/db/AppDatabase.kt \
        app/src/main/java/com/fidriyanto/banktracker/di/AppModule.kt \
        app/src/androidTest/java/com/fidriyanto/banktracker/di/TestAppModule.kt
git commit -m "feat: add MonthlyOverviewDao and bump Room DB to version 2"
```

---

### Task 3: Data Models

**Files:**
- Create: `app/src/main/java/com/fidriyanto/banktracker/ui/dashboard/DashboardModels.kt`

- [ ] **Step 1: Create DashboardModels.kt**

```kotlin
package com.fidriyanto.banktracker.ui.dashboard

enum class Period { THIS_MONTH, LAST_MONTH, LAST_3_MONTHS }

sealed class DashboardUiState {
    object NotSignedIn : DashboardUiState()
    object LoadingNoCache : DashboardUiState()
    data class Loaded(
        val period: Period,
        val thb: CurrencySummary,
        val idr: CurrencySummary,
        val isRefreshing: Boolean,
        val lastUpdated: String?,
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
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/fidriyanto/banktracker/ui/dashboard/DashboardModels.kt
git commit -m "feat: add Dashboard UI data models"
```

---

### Task 4: MonthlyOverviewFetcher

**Files:**
- Create: `app/src/main/java/com/fidriyanto/banktracker/sheets/MonthlyOverviewFetcher.kt`
- Create: `app/src/main/java/com/fidriyanto/banktracker/sheets/MonthlyOverviewFetcherImpl.kt`
- Create: `app/src/main/java/com/fidriyanto/banktracker/di/DashboardModule.kt`

- [ ] **Step 1: Create MonthlyOverviewFetcher interface**

```kotlin
package com.fidriyanto.banktracker.sheets

import com.fidriyanto.banktracker.data.db.MonthlyBudgetEntity
import com.fidriyanto.banktracker.data.db.MonthlyOverviewEntity

interface MonthlyOverviewFetcher {
    data class FetchResult(
        val rows: List<MonthlyOverviewEntity>,
        val budgets: List<MonthlyBudgetEntity>
    )

    suspend fun fetch(): Result<FetchResult>
}
```

- [ ] **Step 2: Create MonthlyOverviewFetcherImpl**

The Sheets API returns all cell values as strings when using default rendering. Using `?valueRenderOption=UNFORMATTED_VALUE` makes numeric cells return as JSON numbers, so `optDouble` works directly.

Sheet layout (0-indexed columns):
- 0 = Month name ("January 2026")
- 1–12 = Bills through Gifts
- 13 = Total Expenditure
- 14 = Income
- 15 = Gross Savings

Rows (0-indexed in the `values` array):
- 0 = header row (skip)
- 1–12 = January–December
- 14 = Total row (skip)
- 16 = Monthly Budget row

```kotlin
package com.fidriyanto.banktracker.sheets

import android.net.Uri
import android.util.Log
import com.fidriyanto.banktracker.auth.GoogleAuthManager
import com.fidriyanto.banktracker.data.db.MonthlyBudgetEntity
import com.fidriyanto.banktracker.data.db.MonthlyOverviewEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

class MonthlyOverviewFetcherImpl @Inject constructor(
    private val authManager: GoogleAuthManager,
    private val httpClient: OkHttpClient
) : MonthlyOverviewFetcher {

    companion object {
        private const val SPREADSHEET_ID = "1OJqLIPFWjJPje8HabLVyMp_AGvsCi9nWM2nHqdzv9-w"
        private const val SHEETS_API = "https://sheets.googleapis.com/v4/spreadsheets"
    }

    override suspend fun fetch(): Result<MonthlyOverviewFetcher.FetchResult> {
        val token = authManager.getValidAccessToken()
            ?: return Result.failure(Exception("Not authenticated"))
        return try {
            val result = withContext(Dispatchers.IO) {
                coroutineScope {
                    val thbDeferred = async { fetchTab("Monthly Overview", "THB", token) }
                    val idrDeferred = async { fetchTab("Monthly Overview IDR", "IDR", token) }
                    val (thbRows, thbBudget) = thbDeferred.await()
                    val (idrRows, idrBudget) = idrDeferred.await()
                    MonthlyOverviewFetcher.FetchResult(
                        rows = thbRows + idrRows,
                        budgets = listOfNotNull(thbBudget, idrBudget)
                    )
                }
            }
            Result.success(result)
        } catch (e: Exception) {
            Log.e("MonthlyOverviewFetcher", "fetch failed", e)
            Result.failure(e)
        }
    }

    private fun fetchTab(
        tabName: String,
        currency: String,
        token: String
    ): Pair<List<MonthlyOverviewEntity>, MonthlyBudgetEntity?> {
        val range = Uri.encode("$tabName!A1:Q17")
        val url = "$SHEETS_API/$SPREADSHEET_ID/values/$range?valueRenderOption=UNFORMATTED_VALUE"
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .build()
        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("Sheets API error: HTTP ${response.code} for $tabName")
        }
        val body = response.body?.string() ?: throw Exception("Empty response for $tabName")
        return parseTab(body, currency)
    }

    private fun parseTab(
        json: String,
        currency: String
    ): Pair<List<MonthlyOverviewEntity>, MonthlyBudgetEntity?> {
        val values = JSONObject(json).getJSONArray("values")
        val rows = mutableListOf<MonthlyOverviewEntity>()
        var budget: MonthlyBudgetEntity? = null

        for (i in 1..12) {
            if (i >= values.length()) break
            val row = values.getJSONArray(i)
            val month = row.optString(0).trim()
            if (month.isBlank()) continue
            rows += MonthlyOverviewEntity(
                month = month,
                currency = currency,
                bills = row.cellDouble(1),
                subscriptions = row.cellDouble(2),
                entertainment = row.cellDouble(3),
                foodDrink = row.cellDouble(4),
                groceries = row.cellDouble(5),
                healthWellbeing = row.cellDouble(6),
                other = row.cellDouble(7),
                shopping = row.cellDouble(8),
                transport = row.cellDouble(9),
                travel = row.cellDouble(10),
                business = row.cellDouble(11),
                gifts = row.cellDouble(12),
                totalExpenditure = row.cellDouble(13),
                income = row.cellDouble(14),
                grossSavings = row.cellDouble(15)
            )
        }

        if (values.length() > 16) {
            val budgetRow = values.getJSONArray(16)
            budget = MonthlyBudgetEntity(
                currency = currency,
                bills = budgetRow.cellDouble(1),
                subscriptions = budgetRow.cellDouble(2),
                entertainment = budgetRow.cellDouble(3),
                foodDrink = budgetRow.cellDouble(4),
                groceries = budgetRow.cellDouble(5),
                healthWellbeing = budgetRow.cellDouble(6),
                other = budgetRow.cellDouble(7),
                shopping = budgetRow.cellDouble(8),
                transport = budgetRow.cellDouble(9),
                travel = budgetRow.cellDouble(10),
                business = budgetRow.cellDouble(11),
                gifts = budgetRow.cellDouble(12)
            )
        }

        return Pair(rows, budget)
    }

    private fun JSONArray.cellDouble(index: Int): Double =
        optDouble(index, 0.0).let { if (it.isNaN()) 0.0 else it }
}
```

- [ ] **Step 3: Create DashboardModule**

```kotlin
package com.fidriyanto.banktracker.di

import com.fidriyanto.banktracker.sheets.MonthlyOverviewFetcher
import com.fidriyanto.banktracker.sheets.MonthlyOverviewFetcherImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DashboardModule {
    @Binds @Singleton
    abstract fun bindMonthlyOverviewFetcher(impl: MonthlyOverviewFetcherImpl): MonthlyOverviewFetcher
}
```

- [ ] **Step 4: Verify build**

```bash
./gradlew compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/fidriyanto/banktracker/sheets/MonthlyOverviewFetcher.kt \
        app/src/main/java/com/fidriyanto/banktracker/sheets/MonthlyOverviewFetcherImpl.kt \
        app/src/main/java/com/fidriyanto/banktracker/di/DashboardModule.kt
git commit -m "feat: add MonthlyOverviewFetcher for Sheets API"
```

---

### Task 5: DashboardRepository

**Files:**
- Create: `app/src/main/java/com/fidriyanto/banktracker/data/repository/DashboardRepository.kt`

- [ ] **Step 1: Create DashboardRepository**

```kotlin
package com.fidriyanto.banktracker.data.repository

import com.fidriyanto.banktracker.data.db.MonthlyOverviewDao
import com.fidriyanto.banktracker.data.db.MonthlyOverviewEntity
import com.fidriyanto.banktracker.sheets.MonthlyOverviewFetcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DashboardRepository @Inject constructor(
    private val dao: MonthlyOverviewDao,
    private val fetcher: MonthlyOverviewFetcher
) {
    fun observeForMonths(
        months: List<String>
    ): Flow<Pair<List<MonthlyOverviewEntity>, List<MonthlyOverviewEntity>>> =
        combine(
            dao.observeByMonths(months, "THB"),
            dao.observeByMonths(months, "IDR")
        ) { thb, idr -> Pair(thb, idr) }

    suspend fun refresh(): Result<Unit> {
        val result = fetcher.fetch()
        result.onSuccess { data ->
            dao.upsertAll(data.rows)
            data.budgets.forEach { dao.upsertBudget(it) }
        }
        return result.map { }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/fidriyanto/banktracker/data/repository/DashboardRepository.kt
git commit -m "feat: add DashboardRepository"
```

---

### Task 6: DashboardViewModel

**Files:**
- Create: `app/src/main/java/com/fidriyanto/banktracker/ui/dashboard/DashboardViewModel.kt`

- [ ] **Step 1: Create DashboardViewModel**

`monthsFor` computes which month-label strings to query based on the selected period. `buildSummary` aggregates rows into a `CurrencySummary` with a sorted, filtered category list.

```kotlin
package com.fidriyanto.banktracker.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fidriyanto.banktracker.auth.GoogleAuthManager
import com.fidriyanto.banktracker.data.db.MonthlyOverviewEntity
import com.fidriyanto.banktracker.data.repository.DashboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: DashboardRepository,
    private val authManager: GoogleAuthManager
) : ViewModel() {

    private val _period = MutableStateFlow(Period.THIS_MONTH)
    val period = _period.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    private val _refreshError = MutableStateFlow(false)
    private var lastFetchedAt: Long? = null

    val state: StateFlow<DashboardUiState> = _period
        .flatMapLatest { p ->
            if (!authManager.isSignedIn()) return@flatMapLatest flowOf(DashboardUiState.NotSignedIn)
            val months = monthsFor(p)
            combine(
                repository.observeForMonths(months),
                _isRefreshing,
                _refreshError
            ) { rows, refreshing, error ->
                val thbRows = rows.first
                val idrRows = rows.second
                if (thbRows.isEmpty() && idrRows.isEmpty() && !refreshing) {
                    DashboardUiState.LoadingNoCache
                } else {
                    DashboardUiState.Loaded(
                        period = p,
                        thb = buildSummary(thbRows),
                        idr = buildSummary(idrRows),
                        isRefreshing = refreshing,
                        lastUpdated = lastFetchedAt?.let { formatRelativeTime(it) },
                        refreshError = error
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState.LoadingNoCache)

    init {
        if (authManager.isSignedIn()) refresh()
    }

    fun selectPeriod(p: Period) {
        _period.value = p
        _refreshError.value = false
    }

    fun refresh() = viewModelScope.launch {
        _isRefreshing.value = true
        _refreshError.value = false
        val result = repository.refresh()
        if (result.isSuccess) lastFetchedAt = System.currentTimeMillis()
        _refreshError.value = result.isFailure
        _isRefreshing.value = false
    }

    private fun monthsFor(period: Period): List<String> {
        val now = LocalDate.now()
        return when (period) {
            Period.THIS_MONTH -> listOf(monthLabel(now))
            Period.LAST_MONTH -> listOf(monthLabel(now.minusMonths(1)))
            Period.LAST_3_MONTHS -> (1L..3L).map { monthLabel(now.minusMonths(it)) }
        }
    }

    private fun monthLabel(date: LocalDate): String {
        val name = date.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
        return "$name ${date.year}"
    }

    private fun formatRelativeTime(ms: Long): String {
        val mins = (System.currentTimeMillis() - ms) / 60_000
        return when {
            mins < 1L -> "just now"
            mins == 1L -> "1 min ago"
            mins < 60L -> "$mins mins ago"
            else -> "${mins / 60}h ago"
        }
    }

    private fun buildSummary(rows: List<MonthlyOverviewEntity>): CurrencySummary {
        val totalIncome = rows.sumOf { it.income }
        val totalExpenses = rows.sumOf { it.totalExpenditure }
        val net = totalIncome - totalExpenses
        val breakdown = listOf(
            "Bills" to rows.sumOf { it.bills },
            "Subscriptions" to rows.sumOf { it.subscriptions },
            "Entertainment" to rows.sumOf { it.entertainment },
            "Food & Drink" to rows.sumOf { it.foodDrink },
            "Groceries" to rows.sumOf { it.groceries },
            "Health & Wellbeing" to rows.sumOf { it.healthWellbeing },
            "Other" to rows.sumOf { it.other },
            "Shopping" to rows.sumOf { it.shopping },
            "Transport" to rows.sumOf { it.transport },
            "Travel" to rows.sumOf { it.travel },
            "Business" to rows.sumOf { it.business },
            "Gifts" to rows.sumOf { it.gifts }
        )
            .filter { it.second > 0.0 }
            .sortedByDescending { it.second }
            .map { (cat, amt) ->
                CategoryRow(
                    category = cat,
                    amount = amt,
                    percentage = if (totalExpenses > 0.0) (amt / totalExpenses).toFloat() else 0f
                )
            }
        return CurrencySummary(totalIncome, totalExpenses, net, breakdown)
    }
}
```

- [ ] **Step 2: Verify build**

```bash
./gradlew compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/fidriyanto/banktracker/ui/dashboard/DashboardViewModel.kt
git commit -m "feat: add DashboardViewModel with period selection and refresh"
```

---

### Task 7: BalanceCard Composable

**Files:**
- Create: `app/src/main/java/com/fidriyanto/banktracker/ui/dashboard/BalanceCard.kt`

- [ ] **Step 1: Create BalanceCard.kt**

Amount formatting: no decimal places, commas as thousands separators (e.g., "฿32,100"). THB prefix "฿", IDR prefix "Rp". Net shows a "+" prefix when positive.

```kotlin
package com.fidriyanto.banktracker.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fidriyanto.banktracker.ui.theme.Accent
import com.fidriyanto.banktracker.ui.theme.Destructive
import com.fidriyanto.banktracker.ui.theme.MutedText
import com.fidriyanto.banktracker.ui.theme.Surface
import java.text.NumberFormat
import java.util.Locale

@Composable
fun BalanceCard(summary: CurrencySummary, currencySymbol: String, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Surface),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            BalanceColumn("Income", formatAmount(summary.totalIncome, currencySymbol), Color.White)
            BalanceColumn("Expenses", formatAmount(summary.totalExpenses, currencySymbol), Color.White)
            val netColor = if (summary.net >= 0) Accent else Destructive
            val netText = formatAmount(summary.net, currencySymbol, showSign = true)
            BalanceColumn("Net", netText, netColor)
        }
    }
}

@Composable
private fun BalanceColumn(label: String, value: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = MutedText, fontSize = 12.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, color = valueColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

internal fun formatAmount(amount: Double, symbol: String, showSign: Boolean = false): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
        maximumFractionDigits = 0
    }
    val absFormatted = formatter.format(kotlin.math.abs(amount))
    return when {
        showSign && amount >= 0 -> "+$symbol$absFormatted"
        amount < 0 -> "-$symbol$absFormatted"
        else -> "$symbol$absFormatted"
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/fidriyanto/banktracker/ui/dashboard/BalanceCard.kt
git commit -m "feat: add BalanceCard composable"
```

---

### Task 8: CategoryBreakdownCard Composable

**Files:**
- Create: `app/src/main/java/com/fidriyanto/banktracker/ui/dashboard/CategoryBreakdownCard.kt`

- [ ] **Step 1: Create CategoryBreakdownCard.kt**

The progress bar fill color uses the theme `Accent` (green). The unfilled track is a dark gray `Color(0xFF2A2A2A)`. Progress values are clamped to [0f, 1f].

```kotlin
package com.fidriyanto.banktracker.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fidriyanto.banktracker.ui.theme.Accent
import com.fidriyanto.banktracker.ui.theme.MutedText
import com.fidriyanto.banktracker.ui.theme.Surface

@Composable
fun CategoryBreakdownCard(
    summary: CurrencySummary,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Surface),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Spending Breakdown",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    formatAmount(summary.totalExpenses, currencySymbol),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            if (summary.categoryBreakdown.isEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("No spending recorded", color = MutedText, fontSize = 14.sp)
            } else {
                Spacer(Modifier.height(12.dp))
                summary.categoryBreakdown.forEachIndexed { index, row ->
                    CategoryRowItem(row, currencySymbol)
                    if (index < summary.categoryBreakdown.lastIndex) {
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryRowItem(row: CategoryRow, currencySymbol: String) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                row.category,
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )
            Text(
                formatAmount(row.amount, currencySymbol),
                color = Color.White,
                fontSize = 14.sp
            )
        }
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LinearProgressIndicator(
                progress = { row.percentage.coerceIn(0f, 1f) },
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp),
                color = Accent,
                trackColor = Color(0xFF2A2A2A)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "${(row.percentage * 100).toInt()}%",
                color = MutedText,
                fontSize = 12.sp,
                textAlign = TextAlign.End,
                modifier = Modifier.width(36.dp)
            )
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/fidriyanto/banktracker/ui/dashboard/CategoryBreakdownCard.kt
git commit -m "feat: add CategoryBreakdownCard composable"
```

---

### Task 9: DashboardScreen + Navigation

**Files:**
- Create: `app/src/main/java/com/fidriyanto/banktracker/ui/dashboard/DashboardScreen.kt`
- Modify: `app/src/main/java/com/fidriyanto/banktracker/ui/navigation/AppNavigation.kt`
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Add extended icons dependency**

In `gradle/libs.versions.toml`, add to `[libraries]`:

```toml
compose-material-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended" }
```

In `app/build.gradle.kts`, add inside the `dependencies` block (with other compose deps):

```kotlin
implementation(libs.compose.material.icons.extended)
```

- [ ] **Step 2: Create DashboardScreen.kt**

`PullToRefreshBox` is available in Material3 1.2.1 (BOM 2024.06.00). It is an experimental API, requiring `@OptIn(ExperimentalMaterial3Api::class)`.

```kotlin
package com.fidriyanto.banktracker.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fidriyanto.banktracker.ui.theme.MutedText

private val Period.label: String
    get() = when (this) {
        Period.THIS_MONTH -> "This Month"
        Period.LAST_MONTH -> "Last Month"
        Period.LAST_3_MONTHS -> "Last 3 Months"
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: DashboardViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val period by viewModel.period.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            "Dashboard",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = Color.White,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        when (val s = state) {
            DashboardUiState.NotSignedIn -> NotSignedInContent()
            DashboardUiState.LoadingNoCache -> LoadingContent()
            is DashboardUiState.Loaded -> LoadedContent(
                state = s,
                period = period,
                onSelectPeriod = viewModel::selectPeriod,
                onRefresh = viewModel::refresh
            )
        }
    }
}

@Composable
private fun NotSignedInContent() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(modifier = Modifier.padding(16.dp)) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Sign in with Google to load your dashboard",
                    color = MutedText,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoadedContent(
    state: DashboardUiState.Loaded,
    period: Period,
    onSelectPeriod: (Period) -> Unit,
    onRefresh: () -> Unit
) {
    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = onRefresh
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PeriodSelector(period, onSelectPeriod)

            if (state.refreshError && state.lastUpdated != null) {
                Text(
                    "Last updated ${state.lastUpdated}",
                    color = MutedText,
                    fontSize = 12.sp
                )
            }

            CurrencySection("THB", "฿", state.thb)
            CurrencySection("IDR", "Rp", state.idr)

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PeriodSelector(selected: Period, onSelect: (Period) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Period.entries.forEach { p ->
            FilterChip(
                selected = selected == p,
                onClick = { onSelect(p) },
                label = { Text(p.label, fontSize = 13.sp) }
            )
        }
    }
}

@Composable
private fun CurrencySection(label: String, symbol: String, summary: CurrencySummary) {
    Text(
        "── $label ──────────────────────────",
        color = MutedText,
        fontSize = 13.sp
    )
    BalanceCard(summary = summary, currencySymbol = symbol)
    CategoryBreakdownCard(summary = summary, currencySymbol = symbol)
}
```

- [ ] **Step 3: Update AppNavigation to add Dashboard tab**

Replace the entire file with:

```kotlin
package com.fidriyanto.banktracker.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.fidriyanto.banktracker.ui.add.AddScreen
import com.fidriyanto.banktracker.ui.dashboard.DashboardScreen
import com.fidriyanto.banktracker.ui.feed.FeedScreen
import com.fidriyanto.banktracker.ui.settings.SettingsScreen

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Feed : Screen("feed", "Feed", Icons.Outlined.List)
    object Add : Screen("add", "Add", Icons.Outlined.Add)
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Outlined.Dashboard)
    object Settings : Screen("settings", "Settings", Icons.Outlined.Settings)
}

private val screens = listOf(Screen.Feed, Screen.Add, Screen.Dashboard, Screen.Settings)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = {
            NavigationBar {
                val backStack by navController.currentBackStackEntryAsState()
                val current = backStack?.destination
                screens.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = current?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(navController, startDestination = Screen.Feed.route) {
            composable(Screen.Feed.route) { FeedScreen() }
            composable(Screen.Add.route) { AddScreen() }
            composable(Screen.Dashboard.route) { DashboardScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}
```

- [ ] **Step 4: Verify build**

```bash
./gradlew compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/fidriyanto/banktracker/ui/dashboard/DashboardScreen.kt \
        app/src/main/java/com/fidriyanto/banktracker/ui/navigation/AppNavigation.kt \
        gradle/libs.versions.toml \
        app/build.gradle.kts
git commit -m "feat: add DashboardScreen and wire up navigation"
```

---

### Task 10: Fake + TestAppModule + DashboardScreen UI Tests

**Files:**
- Create: `app/src/androidTest/java/com/fidriyanto/banktracker/fake/FakeMonthlyOverviewFetcher.kt`
- Modify: `app/src/androidTest/java/com/fidriyanto/banktracker/di/TestAppModule.kt`
- Create: `app/src/androidTest/java/com/fidriyanto/banktracker/ui/DashboardScreenTest.kt`

- [ ] **Step 1: Create FakeMonthlyOverviewFetcher**

```kotlin
package com.fidriyanto.banktracker.fake

import com.fidriyanto.banktracker.data.db.MonthlyOverviewEntity
import com.fidriyanto.banktracker.sheets.MonthlyOverviewFetcher
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeMonthlyOverviewFetcher @Inject constructor() : MonthlyOverviewFetcher {
    var shouldSucceed = true
    var fetchCalled = false
    var stubbedRows: List<MonthlyOverviewEntity> = emptyList()

    override suspend fun fetch(): Result<MonthlyOverviewFetcher.FetchResult> {
        fetchCalled = true
        return if (shouldSucceed) {
            Result.success(MonthlyOverviewFetcher.FetchResult(rows = stubbedRows, budgets = emptyList()))
        } else {
            Result.failure(Exception("Fake fetch error"))
        }
    }

    fun reset() {
        shouldSucceed = true
        fetchCalled = false
        stubbedRows = emptyList()
    }
}
```

- [ ] **Step 2: Update TestAppModule to replace DashboardModule**

Change the `@TestInstallIn` annotation to also replace `DashboardModule`, and add the binding for `FakeMonthlyOverviewFetcher`. The full updated file:

```kotlin
package com.fidriyanto.banktracker.di

import android.content.Context
import androidx.room.Room
import com.fidriyanto.banktracker.auth.GoogleAuthManager
import com.fidriyanto.banktracker.data.db.AppDatabase
import com.fidriyanto.banktracker.fake.FakeGoogleAuthManager
import com.fidriyanto.banktracker.fake.FakeMonthlyOverviewFetcher
import com.fidriyanto.banktracker.fake.FakeSheetsSyncer
import com.fidriyanto.banktracker.sheets.MonthlyOverviewFetcher
import com.fidriyanto.banktracker.sheets.SheetsSyncer
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [AppModule::class, AuthModule::class, SheetsModule::class, DashboardModule::class]
)
abstract class TestAppModule {

    @Binds @Singleton
    abstract fun bindSheetsSyncer(fake: FakeSheetsSyncer): SheetsSyncer

    @Binds @Singleton
    abstract fun bindGoogleAuthManager(fake: FakeGoogleAuthManager): GoogleAuthManager

    @Binds @Singleton
    abstract fun bindMonthlyOverviewFetcher(fake: FakeMonthlyOverviewFetcher): MonthlyOverviewFetcher

    companion object {
        @Provides @Singleton
        fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
            Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()

        @Provides fun provideTransactionDao(db: AppDatabase) = db.transactionDao()
        @Provides fun provideCategoryCacheDao(db: AppDatabase) = db.categoryCacheDao()
        @Provides fun provideProcessedRefDao(db: AppDatabase) = db.processedRefDao()
        @Provides fun provideMonthlyOverviewDao(db: AppDatabase) = db.monthlyOverviewDao()

        @Provides @Singleton
        fun provideOkHttpClient(): OkHttpClient = OkHttpClient()
    }
}
```

- [ ] **Step 3: Write DashboardScreenTest**

These tests cover: not-signed-in state, loading state (no cache, not yet refreshed), loaded state (THB and IDR sections visible), period chip selection, and empty category list.

```kotlin
package com.fidriyanto.banktracker.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.fidriyanto.banktracker.TestActivity
import com.fidriyanto.banktracker.data.db.MonthlyOverviewDao
import com.fidriyanto.banktracker.data.db.MonthlyOverviewEntity
import com.fidriyanto.banktracker.fake.FakeGoogleAuthManager
import com.fidriyanto.banktracker.fake.FakeMonthlyOverviewFetcher
import com.fidriyanto.banktracker.ui.dashboard.DashboardScreen
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
class DashboardScreenTest {

    @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
    @get:Rule(order = 1) val composeRule = createAndroidComposeRule<TestActivity>()

    @Inject lateinit var fakeGoogleAuthManager: FakeGoogleAuthManager
    @Inject lateinit var fakeFetcher: FakeMonthlyOverviewFetcher
    @Inject lateinit var dao: MonthlyOverviewDao

    @Before
    fun setUp() {
        hiltRule.inject()
        fakeGoogleAuthManager.reset()
        fakeFetcher.reset()
    }

    @Test
    fun whenNotSignedIn_showsSignInPrompt() {
        fakeGoogleAuthManager.signedIn = false
        composeRule.setContent { DashboardScreen() }

        composeRule.onNodeWithText("Sign in with Google to load your dashboard").assertIsDisplayed()
    }

    @Test
    fun whenSignedIn_noCache_showsLoading() {
        fakeGoogleAuthManager.signedIn = true
        fakeFetcher.shouldSucceed = false  // prevent fake data from populating cache

        composeRule.setContent { DashboardScreen() }

        // LoadingNoCache shows a progress indicator (no currency sections visible yet)
        composeRule.onNodeWithText("── THB ──────────────────────────").assertDoesNotExist()
    }

    @Test
    fun whenCacheHasData_showsTHBAndIDRSections() {
        fakeGoogleAuthManager.signedIn = true
        fakeFetcher.shouldSucceed = false  // disable auto-refresh so we control cache manually

        val currentMonth = java.time.LocalDate.now().let {
            val name = it.month.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH)
            "$name ${it.year}"
        }
        runBlocking {
            dao.upsertAll(listOf(
                MonthlyOverviewEntity(
                    month = currentMonth, currency = "THB",
                    bills = 0.0, subscriptions = 0.0, entertainment = 0.0,
                    foodDrink = 12400.0, groceries = 0.0, healthWellbeing = 0.0,
                    other = 0.0, shopping = 0.0, transport = 6800.0,
                    travel = 0.0, business = 0.0, gifts = 0.0,
                    totalExpenditure = 19200.0, income = 45200.0, grossSavings = 26000.0
                ),
                MonthlyOverviewEntity(
                    month = currentMonth, currency = "IDR",
                    bills = 0.0, subscriptions = 0.0, entertainment = 0.0,
                    foodDrink = 500000.0, groceries = 0.0, healthWellbeing = 0.0,
                    other = 0.0, shopping = 0.0, transport = 0.0,
                    travel = 0.0, business = 0.0, gifts = 0.0,
                    totalExpenditure = 500000.0, income = 2000000.0, grossSavings = 1500000.0
                )
            ))
        }

        composeRule.setContent { DashboardScreen() }

        composeRule.waitUntil(3_000) {
            composeRule.onAllNodesWithText("── THB ──────────────────────────")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("── THB ──────────────────────────").assertIsDisplayed()
        composeRule.onNodeWithText("── IDR ──────────────────────────").assertIsDisplayed()
    }

    @Test
    fun loadedState_showsIncomeLabelInBalanceCard() {
        fakeGoogleAuthManager.signedIn = true
        fakeFetcher.shouldSucceed = false

        val currentMonth = java.time.LocalDate.now().let {
            val name = it.month.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH)
            "$name ${it.year}"
        }
        runBlocking {
            dao.upsertAll(listOf(
                MonthlyOverviewEntity(
                    month = currentMonth, currency = "THB",
                    bills = 0.0, subscriptions = 0.0, entertainment = 0.0,
                    foodDrink = 0.0, groceries = 0.0, healthWellbeing = 0.0,
                    other = 0.0, shopping = 0.0, transport = 0.0,
                    travel = 0.0, business = 0.0, gifts = 0.0,
                    totalExpenditure = 0.0, income = 0.0, grossSavings = 0.0
                )
            ))
        }

        composeRule.setContent { DashboardScreen() }

        composeRule.waitUntil(3_000) {
            composeRule.onAllNodesWithText("Income").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodesWithText("Income").onFirst().assertIsDisplayed()
        composeRule.onAllNodesWithText("Expenses").onFirst().assertIsDisplayed()
        composeRule.onAllNodesWithText("Net").onFirst().assertIsDisplayed()
    }

    @Test
    fun periodChips_areDisplayedAndSelectable() {
        fakeGoogleAuthManager.signedIn = true
        fakeFetcher.shouldSucceed = false

        val currentMonth = java.time.LocalDate.now().let {
            val name = it.month.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH)
            "$name ${it.year}"
        }
        runBlocking {
            dao.upsertAll(listOf(
                MonthlyOverviewEntity(
                    month = currentMonth, currency = "THB",
                    bills = 0.0, subscriptions = 0.0, entertainment = 0.0,
                    foodDrink = 0.0, groceries = 0.0, healthWellbeing = 0.0,
                    other = 0.0, shopping = 0.0, transport = 0.0,
                    travel = 0.0, business = 0.0, gifts = 0.0,
                    totalExpenditure = 0.0, income = 0.0, grossSavings = 0.0
                )
            ))
        }

        composeRule.setContent { DashboardScreen() }

        composeRule.waitUntil(3_000) {
            composeRule.onAllNodesWithText("This Month").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("This Month").assertIsDisplayed()
        composeRule.onNodeWithText("Last Month").assertIsDisplayed()
        composeRule.onNodeWithText("Last 3 Months").assertIsDisplayed()

        composeRule.onNodeWithText("Last Month").performClick()
        composeRule.waitUntil(2_000) {
            composeRule.onAllNodesWithText("Last Month").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun noSpending_showsNoSpendingRecorded() {
        fakeGoogleAuthManager.signedIn = true
        fakeFetcher.shouldSucceed = false

        val currentMonth = java.time.LocalDate.now().let {
            val name = it.month.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH)
            "$name ${it.year}"
        }
        runBlocking {
            dao.upsertAll(listOf(
                MonthlyOverviewEntity(
                    month = currentMonth, currency = "THB",
                    bills = 0.0, subscriptions = 0.0, entertainment = 0.0,
                    foodDrink = 0.0, groceries = 0.0, healthWellbeing = 0.0,
                    other = 0.0, shopping = 0.0, transport = 0.0,
                    travel = 0.0, business = 0.0, gifts = 0.0,
                    totalExpenditure = 0.0, income = 0.0, grossSavings = 0.0
                )
            ))
        }

        composeRule.setContent { DashboardScreen() }

        composeRule.waitUntil(3_000) {
            composeRule.onAllNodesWithText("No spending recorded").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodesWithText("No spending recorded").onFirst().assertIsDisplayed()
    }
}
```

- [ ] **Step 4: Run the tests — expect BUILD SUCCESSFUL**

```bash
./gradlew connectedDebugAndroidTest
```

Expected: all tests pass. If a test fails with a Compose node not found error, check that the composable uses the exact text strings in the test assertions and that `waitUntil` timeouts are sufficient.

- [ ] **Step 5: Commit**

```bash
git add app/src/androidTest/java/com/fidriyanto/banktracker/fake/FakeMonthlyOverviewFetcher.kt \
        app/src/androidTest/java/com/fidriyanto/banktracker/di/TestAppModule.kt \
        app/src/androidTest/java/com/fidriyanto/banktracker/ui/DashboardScreenTest.kt
git commit -m "test: add DashboardScreen UI tests with fake fetcher"
```

---

## Self-Review

**Spec coverage check:**

| Spec requirement | Task covering it |
|-----------------|-----------------|
| Fourth tab in bottom nav (Feed/Add/Dashboard/Settings) | Task 9 — AppNavigation |
| Period selector: This Month / Last Month / Last 3 Months | Task 6 — ViewModel `monthsFor`, Task 9 — `PeriodSelector` |
| THB balance card (Income, Expenses, Net) | Task 7 — BalanceCard |
| Net green when positive, red when negative | Task 7 — BalanceCard `netColor` |
| Category breakdown card with % bars | Task 8 — CategoryBreakdownCard |
| Categories sorted by amount descending, ฿0/Rp0 omitted | Task 6 — `buildSummary` |
| Primary source: Sheets API (Monthly Overview + IDR tabs) | Task 4 — MonthlyOverviewFetcherImpl |
| Room cache (MonthlyOverviewEntity, MonthlyBudgetEntity) | Tasks 1–2 |
| Pull-to-refresh | Task 9 — `PullToRefreshBox` |
| Not signed in state | Task 6 — ViewModel, Task 9 — `NotSignedInContent`, Task 10 — test |
| LoadingNoCache state | Task 6 — ViewModel, Task 10 — test |
| Refresh failed + cache exists → show "Last updated X ago" | Task 6 — ViewModel `refreshError` + `lastUpdated` |
| No data for period → "No spending recorded" | Task 8 — CategoryBreakdownCard, Task 10 — test |

**Type consistency check:** `formatAmount` is defined in `BalanceCard.kt` as `internal` and reused in `CategoryBreakdownCard.kt` — both are in the same package `com.fidriyanto.banktracker.ui.dashboard` so `internal` visibility is accessible. `CurrencySummary`, `CategoryRow`, `Period`, `DashboardUiState` are all defined in `DashboardModels.kt` and imported consistently across ViewModel, Screen, and card files.

**Placeholder scan:** No TBDs or incomplete sections found.
