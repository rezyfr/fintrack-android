# Notification-Only Pipeline Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove Gmail email fetching entirely and parse transactions directly from push notification data, eliminating the race-condition root cause of duplicate/wrong-amount transactions.

**Architecture:** `BankNotificationService` reads `EXTRA_TITLE` + `EXTRA_TEXT` from each notification, passes them to a new `NotificationParser` which produces a `ParsedTransaction`, then calls `TransactionRepository.processNewNotification(ParsedTransaction)` directly in a coroutine — no WorkManager, no network, no Gmail API. Deduplication uses a composite key (`merchant|amount|dateIso`) in `processed_refs` since notifications never carry a reference number.

**Tech Stack:** Kotlin, Room, Hilt, Coroutines, JUnit + MockK (unit tests)

---

## File Map

| Action | File |
|--------|------|
| Create | `app/src/main/java/com/fidriyanto/banktracker/notification/NotificationParser.kt` |
| Create | `app/src/test/java/com/fidriyanto/banktracker/notification/NotificationParserTest.kt` |
| Move+edit | `email/MerchantNormalizer.kt` → `categorization/MerchantNormalizer.kt` |
| Move+edit | `test/.../email/MerchantNormalizerTest.kt` → `test/.../categorization/MerchantNormalizerTest.kt` |
| Modify | `data/db/ProcessedRefEntity.kt` |
| Modify | `data/db/ProcessedRefDao.kt` |
| Modify | `data/db/AppDatabase.kt` |
| Modify | `data/repository/TransactionRepository.kt` |
| Modify | `categorization/CategoryResolver.kt` |
| Modify | `service/BankNotificationService.kt` |
| Modify | `auth/GoogleAuthManagerImpl.kt` |
| Modify | `test/.../data/repository/TransactionRepositoryTest.kt` |
| Delete | `email/EmailFetcher.kt` |
| Delete | `email/EmailParser.kt` |
| Delete | `service/EmailFetchWorker.kt` |
| Delete | `test/.../email/EmailParserTest.kt` |

---

### Task 1: Move MerchantNormalizer to categorization package

**Files:**
- Create: `app/src/main/java/com/fidriyanto/banktracker/categorization/MerchantNormalizer.kt`
- Create: `app/src/test/java/com/fidriyanto/banktracker/categorization/MerchantNormalizerTest.kt`
- Modify: `app/src/main/java/com/fidriyanto/banktracker/categorization/CategoryResolver.kt`
- Delete: `app/src/main/java/com/fidriyanto/banktracker/email/MerchantNormalizer.kt`
- Delete: `app/src/test/java/com/fidriyanto/banktracker/email/MerchantNormalizerTest.kt`

- [ ] **Step 1: Create MerchantNormalizer in categorization package**

`app/src/main/java/com/fidriyanto/banktracker/categorization/MerchantNormalizer.kt`:
```kotlin
package com.fidriyanto.banktracker.categorization

object MerchantNormalizer {
    private val legalSuffixes = listOf(
        "PUBLIC COMPANY LIMITED", "COMPANY LIMITED",
        "CO., LTD.", "CO.,LTD.", "CO LTD",
        "PCL.", "PCL", "PLC.", "PLC", "LTD.", "LTD"
    )

    fun normalize(raw: String): String {
        var result = raw.uppercase().trim()
        for (suffix in legalSuffixes) {
            if (result.endsWith(suffix)) {
                result = result.dropLast(suffix.length).trimEnd(',').trim()
                break
            }
        }
        return result.replace("'", "").replace(".", "").trim()
    }
}
```

- [ ] **Step 2: Create MerchantNormalizerTest in categorization package**

`app/src/test/java/com/fidriyanto/banktracker/categorization/MerchantNormalizerTest.kt`:
```kotlin
package com.fidriyanto.banktracker.categorization

import org.junit.Assert.assertEquals
import org.junit.Test

class MerchantNormalizerTest {
    @Test fun `strips CO LTD suffix`() =
        assertEquals("TRUE MONEY", MerchantNormalizer.normalize("TRUE MONEY CO., LTD."))

    @Test fun `strips PCL suffix`() =
        assertEquals("SOME CORP", MerchantNormalizer.normalize("SOME CORP PCL"))

    @Test fun `strips PLC suffix`() =
        assertEquals("SOME CORP", MerchantNormalizer.normalize("SOME CORP PLC."))

    @Test fun `uppercases and trims`() =
        assertEquals("BTS", MerchantNormalizer.normalize("  bts  "))

    @Test fun `removes dots`() =
        assertEquals("MR JOHN", MerchantNormalizer.normalize("MR. JOHN"))
}
```

- [ ] **Step 3: Run the new test to verify it passes**

```bash
./gradlew :app:testDebugUnitTest --tests "com.fidriyanto.banktracker.categorization.MerchantNormalizerTest" 2>&1 | tail -20
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Update CategoryResolver import**

In `app/src/main/java/com/fidriyanto/banktracker/categorization/CategoryResolver.kt`, change:
```kotlin
import com.fidriyanto.banktracker.email.MerchantNormalizer
```
to:
```kotlin
import com.fidriyanto.banktracker.categorization.MerchantNormalizer
```

- [ ] **Step 5: Delete old files**

```bash
rm app/src/main/java/com/fidriyanto/banktracker/email/MerchantNormalizer.kt
rm app/src/test/java/com/fidriyanto/banktracker/email/MerchantNormalizerTest.kt
```

- [ ] **Step 6: Verify project still compiles**

```bash
./gradlew :app:compileDebugKotlin 2>&1 | tail -20
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "refactor: move MerchantNormalizer to categorization package"
```

---

### Task 2: Create NotificationParser

**Files:**
- Create: `app/src/main/java/com/fidriyanto/banktracker/notification/NotificationParser.kt`
- Create: `app/src/test/java/com/fidriyanto/banktracker/notification/NotificationParserTest.kt`

- [ ] **Step 1: Write the failing tests**

`app/src/test/java/com/fidriyanto/banktracker/notification/NotificationParserTest.kt`:
```kotlin
package com.fidriyanto.banktracker.notification

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class NotificationParserTest {

    // 2026-05-24T00:00:00Z = 2026-05-24 07:00 Asia/Bangkok — still May 24
    private val may24EpochMs = 1779580800000L

    @Test
    fun `parses bill payment notification`() {
        val result = NotificationParser.parse(
            title = "ชำระบิล / Bill Payment",
            text = "BTS TIM TVM 65.00THB",
            timestampMs = may24EpochMs
        )!!
        assertEquals("BTS TIM TVM", result.merchant)
        assertEquals(65.0, result.amount, 0.01)
        assertEquals("BillPayment", result.channel)
        assertEquals(LocalDate.of(2026, 5, 24), result.date)
        assertEquals("", result.referenceNo)
    }

    @Test
    fun `parses ewallet notification`() {
        val result = NotificationParser.parse(
            title = "E-Wallet Transfer",
            text = "K Plus Wallet 40.00THB",
            timestampMs = may24EpochMs
        )!!
        assertEquals("K Plus Wallet", result.merchant)
        assertEquals(40.0, result.amount, 0.01)
        assertEquals("eWallet", result.channel)
    }

    @Test
    fun `parses promptpay notification`() {
        val result = NotificationParser.parse(
            title = "โอนเงิน PromptPay",
            text = "MR MONGKON 226.00THB",
            timestampMs = may24EpochMs
        )!!
        assertEquals("MR MONGKON", result.merchant)
        assertEquals(226.0, result.amount, 0.01)
        assertEquals("PromptPay", result.channel)
    }

    @Test
    fun `parses bank transfer notification`() {
        val result = NotificationParser.parse(
            title = "โอนเงิน / Transfer",
            text = "TTB NITTRA PATTAR 55.00THB",
            timestampMs = may24EpochMs
        )!!
        assertEquals("TTB NITTRA PATTAR", result.merchant)
        assertEquals(55.0, result.amount, 0.01)
        assertEquals("BankTransfer", result.channel)
    }

    @Test
    fun `returns null when title has no recognized channel`() {
        assertNull(NotificationParser.parse(
            title = "Bangkok Bank",
            text = "Something 100.00THB",
            timestampMs = may24EpochMs
        ))
    }

    @Test
    fun `returns null when text has no THB amount`() {
        assertNull(NotificationParser.parse(
            title = "Bill Payment",
            text = "Some merchant without amount",
            timestampMs = may24EpochMs
        ))
    }

    @Test
    fun `returns null when text is null`() {
        assertNull(NotificationParser.parse(
            title = "Bill Payment",
            text = null,
            timestampMs = may24EpochMs
        ))
    }

    @Test
    fun `returns null when title is null`() {
        assertNull(NotificationParser.parse(
            title = null,
            text = "BTS TIM TVM 65.00THB",
            timestampMs = may24EpochMs
        ))
    }

    @Test
    fun `handles comma-separated amounts`() {
        val result = NotificationParser.parse(
            title = "Bill Payment",
            text = "TRUE MONEY CO LTD 1,500.00THB",
            timestampMs = may24EpochMs
        )!!
        assertEquals(1500.0, result.amount, 0.01)
        assertEquals("TRUE MONEY CO LTD", result.merchant)
    }

    @Test
    fun `returns null when merchant is empty`() {
        assertNull(NotificationParser.parse(
            title = "Bill Payment",
            text = "65.00THB",
            timestampMs = may24EpochMs
        ))
    }
}
```

- [ ] **Step 2: Run to confirm compilation failure**

```bash
./gradlew :app:testDebugUnitTest --tests "com.fidriyanto.banktracker.notification.NotificationParserTest" 2>&1 | tail -10
```
Expected: `error: unresolved reference: NotificationParser`

- [ ] **Step 3: Implement NotificationParser**

`app/src/main/java/com/fidriyanto/banktracker/notification/NotificationParser.kt`:
```kotlin
package com.fidriyanto.banktracker.notification

import com.fidriyanto.banktracker.data.model.ParsedTransaction
import java.time.Instant
import java.time.ZoneId
import java.time.LocalDate

object NotificationParser {

    private val amountRegex = Regex("""(\d[\d,]*(?:\.\d{1,2})?)THB""", RegexOption.IGNORE_CASE)
    private val bangkokZone = ZoneId.of("Asia/Bangkok")

    fun parse(title: String?, text: String?, timestampMs: Long): ParsedTransaction? {
        title ?: return null
        text ?: return null
        val channel = detectChannel(title) ?: return null
        val amountMatch = amountRegex.find(text) ?: return null
        val amount = amountMatch.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
        val merchant = text.substring(0, amountMatch.range.first).trim()
        if (merchant.isEmpty()) return null
        val date: LocalDate = Instant.ofEpochMilli(timestampMs)
            .atZone(bangkokZone)
            .toLocalDate()
        return ParsedTransaction(
            merchant = merchant,
            amount = amount,
            date = date,
            channel = channel,
            referenceNo = ""
        )
    }

    private fun detectChannel(title: String): String? {
        val t = title.lowercase()
        return when {
            "bill payment" in t || "ชำระบิล" in t -> "BillPayment"
            "e-wallet" in t || "ewallet" in t -> "eWallet"
            "promptpay" in t || "พร้อมเพย์" in t -> "PromptPay"
            "transfer" in t || "โอนเงิน" in t -> "BankTransfer"
            else -> null
        }
    }
}
```

- [ ] **Step 4: Run tests and verify they pass**

```bash
./gradlew :app:testDebugUnitTest --tests "com.fidriyanto.banktracker.notification.NotificationParserTest" 2>&1 | tail -20
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/fidriyanto/banktracker/notification/NotificationParser.kt \
        app/src/test/java/com/fidriyanto/banktracker/notification/NotificationParserTest.kt
git commit -m "feat: add NotificationParser for direct push notification parsing"
```

---

### Task 3: Update ProcessedRef schema and DB migration

**Files:**
- Modify: `app/src/main/java/com/fidriyanto/banktracker/data/db/ProcessedRefEntity.kt`
- Modify: `app/src/main/java/com/fidriyanto/banktracker/data/db/ProcessedRefDao.kt`
- Modify: `app/src/main/java/com/fidriyanto/banktracker/data/db/AppDatabase.kt`

The `referenceNo` primary key is replaced by `compositeKey` (format: `"merchant|amount|dateIso"`). The DB version bumps from 2 to 3 with a destructive migration of the `processed_refs` table only.

- [ ] **Step 1: Update ProcessedRefEntity**

Replace the full content of `app/src/main/java/com/fidriyanto/banktracker/data/db/ProcessedRefEntity.kt`:
```kotlin
package com.fidriyanto.banktracker.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "processed_refs")
data class ProcessedRefEntity(
    @PrimaryKey val compositeKey: String,
    val processedAt: Long = System.currentTimeMillis()
)
```

- [ ] **Step 2: Update ProcessedRefDao**

Replace the full content of `app/src/main/java/com/fidriyanto/banktracker/data/db/ProcessedRefDao.kt`:
```kotlin
package com.fidriyanto.banktracker.data.db

import androidx.room.*

@Dao
interface ProcessedRefDao {
    @Query("SELECT COUNT(*) FROM processed_refs WHERE compositeKey = :key")
    suspend fun exists(key: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(ref: ProcessedRefEntity)
}
```

- [ ] **Step 3: Update AppDatabase with version 3 and migration**

Replace the full content of `app/src/main/java/com/fidriyanto/banktracker/data/db/AppDatabase.kt`:
```kotlin
package com.fidriyanto.banktracker.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        TransactionEntity::class,
        CategoryCacheEntity::class,
        ProcessedRefEntity::class,
        MonthlyOverviewEntity::class,
        MonthlyBudgetEntity::class
    ],
    version = 3
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryCacheDao(): CategoryCacheDao
    abstract fun processedRefDao(): ProcessedRefDao
    abstract fun monthlyOverviewDao(): MonthlyOverviewDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `processed_refs`")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `processed_refs` " +
                    "(`compositeKey` TEXT NOT NULL, `processedAt` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`compositeKey`))"
                )
            }
        }
    }
}
```

- [ ] **Step 4: Wire migration into AppModule**

In `app/src/main/java/com/fidriyanto/banktracker/di/AppModule.kt`, update `provideDatabase` to use the migration instead of `fallbackToDestructiveMigration`:
```kotlin
@Provides @Singleton
fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
    Room.databaseBuilder(ctx, AppDatabase::class.java, "banktracker.db")
        .addMigrations(AppDatabase.MIGRATION_2_3)
        .build()
```

- [ ] **Step 5: Verify compilation**

```bash
./gradlew :app:compileDebugKotlin 2>&1 | tail -20
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/fidriyanto/banktracker/data/db/ProcessedRefEntity.kt \
        app/src/main/java/com/fidriyanto/banktracker/data/db/ProcessedRefDao.kt \
        app/src/main/java/com/fidriyanto/banktracker/data/db/AppDatabase.kt \
        app/src/main/java/com/fidriyanto/banktracker/di/AppModule.kt
git commit -m "feat: migrate processed_refs to composite key (merchant|amount|date)"
```

---

### Task 4: Update TransactionRepository

**Files:**
- Modify: `app/src/main/java/com/fidriyanto/banktracker/data/repository/TransactionRepository.kt`
- Modify: `app/src/test/java/com/fidriyanto/banktracker/data/repository/TransactionRepositoryTest.kt`

- [ ] **Step 1: Write failing tests for new processNewNotification signature**

Replace the full content of `app/src/test/java/com/fidriyanto/banktracker/data/repository/TransactionRepositoryTest.kt`:
```kotlin
package com.fidriyanto.banktracker.data.repository

import com.fidriyanto.banktracker.categorization.CategoryResolver
import com.fidriyanto.banktracker.categorization.ResolvedCategory
import com.fidriyanto.banktracker.data.db.*
import com.fidriyanto.banktracker.data.model.*
import com.fidriyanto.banktracker.data.prefs.SecurePrefs
import com.fidriyanto.banktracker.sheets.SheetsSyncer
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class TransactionRepositoryTest {

    private val transactionDao = mockk<TransactionDao>(relaxed = true)
    private val processedRefDao = mockk<ProcessedRefDao>(relaxed = true)
    private val categoryResolver = mockk<CategoryResolver>()
    private val sheetsSyncer = mockk<SheetsSyncer>()
    private val prefs = mockk<SecurePrefs>()

    private lateinit var repository: TransactionRepository

    private val existingEntity = TransactionEntity(
        id = 1L,
        merchant = "GRAB",
        item = "GRAB",
        amount = 250.0,
        category = "Transport",
        dateIso = "2026-05-16",
        channel = "PromptPay",
        referenceNo = "",
        tab = SheetTab.EXPENSES,
        status = TransactionStatus.PENDING_EDIT
    )

    private val parsedBts = ParsedTransaction(
        merchant = "BTS TIM TVM",
        amount = 65.0,
        date = LocalDate.of(2026, 5, 24),
        channel = "BillPayment",
        referenceNo = ""
    )

    @Before
    fun setUp() {
        repository = TransactionRepository(
            transactionDao, processedRefDao, categoryResolver, sheetsSyncer, prefs
        )
        every { prefs.promptPayThreshold } returns 25000.0
        every { prefs.claudeApiKey } returns "key"
    }

    @Test
    fun `processNewNotification inserts transaction and returns id`() = runTest {
        val compositeKey = "BTS TIM TVM|65.0|2026-05-24"
        coEvery { processedRefDao.exists(compositeKey) } returns 0
        coEvery { categoryResolver.resolve(parsedBts, 25000.0, "key") } returns
                ResolvedCategory("Transport", "BTS TIM TVM")
        coEvery { transactionDao.insert(any()) } returns 42L

        val id = repository.processNewNotification(parsedBts)

        assertEquals(42L, id)
        coVerify { processedRefDao.insert(ProcessedRefEntity(compositeKey)) }
        coVerify { transactionDao.insert(match {
            it.merchant == "BTS TIM TVM" &&
            it.amount == 65.0 &&
            it.channel == "BillPayment" &&
            it.referenceNo == "" &&
            it.status == TransactionStatus.PENDING_EDIT
        }) }
    }

    @Test
    fun `processNewNotification returns null for duplicate composite key`() = runTest {
        val compositeKey = "BTS TIM TVM|65.0|2026-05-24"
        coEvery { processedRefDao.exists(compositeKey) } returns 1

        val id = repository.processNewNotification(parsedBts)

        assertNull(id)
        coVerify(exactly = 0) { transactionDao.insert(any()) }
    }

    @Test
    fun `updateAndSync updates item and category then syncs`() = runTest {
        coEvery { transactionDao.getById(1L) } returns existingEntity
        coEvery { sheetsSyncer.sync(any()) } returns Result.success(Unit)

        repository.updateAndSync(1L, "Grab Food", "Food & Drink")

        coVerify {
            transactionDao.update(match {
                it.item == "Grab Food" && it.category == "Food & Drink" && it.id == 1L
            })
        }
        coVerify { sheetsSyncer.sync(any()) }
    }

    @Test
    fun `updateAndSync does nothing when entity not found`() = runTest {
        coEvery { transactionDao.getById(99L) } returns null

        repository.updateAndSync(99L, "Edit", "Category")

        coVerify(exactly = 0) { transactionDao.update(any()) }
        coVerify(exactly = 0) { sheetsSyncer.sync(any()) }
    }
}
```

- [ ] **Step 2: Run tests to confirm they fail (compilation error — old constructor)**

```bash
./gradlew :app:testDebugUnitTest --tests "com.fidriyanto.banktracker.data.repository.TransactionRepositoryTest" 2>&1 | tail -15
```
Expected: compilation error about constructor mismatch or unresolved reference.

- [ ] **Step 3: Update TransactionRepository**

Replace the full content of `app/src/main/java/com/fidriyanto/banktracker/data/repository/TransactionRepository.kt`:
```kotlin
package com.fidriyanto.banktracker.data.repository

import com.fidriyanto.banktracker.categorization.CategoryResolver
import com.fidriyanto.banktracker.data.db.*
import com.fidriyanto.banktracker.data.model.*
import com.fidriyanto.banktracker.data.prefs.SecurePrefs
import com.fidriyanto.banktracker.sheets.SheetsSyncer
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    private val processedRefDao: ProcessedRefDao,
    private val categoryResolver: CategoryResolver,
    private val sheetsSyncer: SheetsSyncer,
    private val prefs: SecurePrefs
) {
    fun observeTransactions(): Flow<List<TransactionEntity>> = transactionDao.observeAll()

    suspend fun processNewNotification(parsed: ParsedTransaction): Long? {
        val compositeKey = "${parsed.merchant}|${parsed.amount}|${parsed.date}"
        if (processedRefDao.exists(compositeKey) > 0) return null
        processedRefDao.insert(ProcessedRefEntity(compositeKey))

        val resolved = categoryResolver.resolve(parsed, prefs.promptPayThreshold, prefs.claudeApiKey)

        val entity = TransactionEntity(
            merchant = parsed.merchant,
            item = resolved.description,
            amount = parsed.amount,
            category = resolved.category,
            dateIso = parsed.date.toString(),
            channel = parsed.channel,
            referenceNo = "",
            tab = SheetTab.EXPENSES,
            status = TransactionStatus.PENDING_EDIT
        )
        return transactionDao.insert(entity)
    }

    suspend fun syncTransaction(id: Long): Result<Unit> {
        val entity = transactionDao.getById(id)
            ?: return Result.failure(Exception("Transaction not found"))

        transactionDao.updateStatus(id, TransactionStatus.PENDING_SYNC)
        val row = SheetsRow(
            tab = entity.tab,
            date = LocalDate.parse(entity.dateIso),
            item = entity.item,
            amount = entity.amount,
            category = entity.category
        )
        return sheetsSyncer.sync(row).also { result ->
            val newStatus = if (result.isSuccess) TransactionStatus.SYNCED else TransactionStatus.SYNC_FAILED
            transactionDao.updateStatus(id, newStatus)
        }
    }

    suspend fun insertManual(row: SheetsRow): Result<Unit> {
        val entity = TransactionEntity(
            merchant = row.item,
            item = row.item,
            amount = row.amount,
            category = row.category,
            dateIso = row.date.toString(),
            channel = "Manual",
            referenceNo = "",
            tab = row.tab,
            status = TransactionStatus.PENDING_SYNC
        )
        val id = transactionDao.insert(entity)
        return syncTransaction(id)
    }

    suspend fun updateAndSync(id: Long, item: String, category: String) {
        val entity = transactionDao.getById(id) ?: return
        transactionDao.update(entity.copy(item = item, category = category))
        syncTransaction(id)
    }

    suspend fun retryFailedSyncs() {
        transactionDao.getByStatus(TransactionStatus.SYNC_FAILED).forEach { syncTransaction(it.id) }
        transactionDao.getByStatus(TransactionStatus.PENDING_SYNC).forEach { syncTransaction(it.id) }
    }
}
```

- [ ] **Step 4: Run tests and verify they pass**

```bash
./gradlew :app:testDebugUnitTest --tests "com.fidriyanto.banktracker.data.repository.TransactionRepositoryTest" 2>&1 | tail -20
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/fidriyanto/banktracker/data/repository/TransactionRepository.kt \
        app/src/test/java/com/fidriyanto/banktracker/data/repository/TransactionRepositoryTest.kt
git commit -m "feat: remove email fetching from TransactionRepository, accept ParsedTransaction directly"
```

---

### Task 5: Rewire BankNotificationService

**Files:**
- Modify: `app/src/main/java/com/fidriyanto/banktracker/service/BankNotificationService.kt`

- [ ] **Step 1: Replace BankNotificationService**

Replace the full content of `app/src/main/java/com/fidriyanto/banktracker/service/BankNotificationService.kt`:
```kotlin
package com.fidriyanto.banktracker.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.fidriyanto.banktracker.data.repository.TransactionRepository
import com.fidriyanto.banktracker.notification.NotificationParser
import com.fidriyanto.banktracker.notification.ReviewNotificationManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BankNotificationService : NotificationListenerService() {
    companion object {
        private const val TAG = "BankNLS"
    }

    @Inject lateinit var repository: TransactionRepository
    @Inject lateinit var notificationManager: ReviewNotificationManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val title = sbn.notification.extras
            .getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString()
        val text = sbn.notification.extras
            .getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString()
        Log.d(TAG, "pkg=${sbn.packageName} title=$title text=$text")

        val parsed = NotificationParser.parse(title, text, sbn.postTime)
        Log.d(TAG, "parsed=$parsed")
        parsed ?: return

        scope.launch {
            val id = repository.processNewNotification(parsed) ?: return@launch
            notificationManager.showReviewNotification(id)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
```

- [ ] **Step 2: Verify compilation**

```bash
./gradlew :app:compileDebugKotlin 2>&1 | tail -20
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/fidriyanto/banktracker/service/BankNotificationService.kt
git commit -m "feat: rewire BankNotificationService to parse notifications directly, remove WorkManager email trigger"
```

---

### Task 6: Delete dead code and remove Gmail scope

**Files:**
- Delete: `app/src/main/java/com/fidriyanto/banktracker/email/EmailFetcher.kt`
- Delete: `app/src/main/java/com/fidriyanto/banktracker/email/EmailParser.kt`
- Delete: `app/src/main/java/com/fidriyanto/banktracker/service/EmailFetchWorker.kt`
- Delete: `app/src/test/java/com/fidriyanto/banktracker/email/EmailParserTest.kt`
- Modify: `app/src/main/java/com/fidriyanto/banktracker/auth/GoogleAuthManagerImpl.kt`

- [ ] **Step 1: Delete dead files**

```bash
rm app/src/main/java/com/fidriyanto/banktracker/email/EmailFetcher.kt
rm app/src/main/java/com/fidriyanto/banktracker/email/EmailParser.kt
rm app/src/main/java/com/fidriyanto/banktracker/service/EmailFetchWorker.kt
rm app/src/test/java/com/fidriyanto/banktracker/email/EmailParserTest.kt
```

- [ ] **Step 2: Remove gmail.readonly scope from GoogleAuthManagerImpl**

In `app/src/main/java/com/fidriyanto/banktracker/auth/GoogleAuthManagerImpl.kt`, replace the `SCOPES` companion object and `getSignInIntent()` / `getValidAccessToken()` to only use the Sheets scope:

```kotlin
companion object {
    val SCOPES = listOf("https://www.googleapis.com/auth/spreadsheets")
}

override fun getSignInIntent(): Intent {
    val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestScopes(com.google.android.gms.common.api.Scope(SCOPES[0]))
        .build()
    return GoogleSignIn.getClient(context, options).signInIntent
}
```

The `getValidAccessToken()` body does not reference `SCOPES` directly by index — it uses the `credential` which is constructed with `SCOPES`. No other change needed there.

- [ ] **Step 3: Verify full build and all unit tests pass**

```bash
./gradlew :app:testDebugUnitTest 2>&1 | tail -30
```
Expected: `BUILD SUCCESSFUL` with all tests passing (no EmailParserTest, no EmailFetchWorker references).

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "chore: delete EmailFetcher, EmailParser, EmailFetchWorker; remove gmail.readonly OAuth scope"
```

---

## Self-Review

**Spec coverage check:**

| Spec requirement | Task |
|---|---|
| Remove EmailFetcher, EmailParser, EmailFetchWorker | Task 6 |
| Create NotificationParser (title+text+timestamp → ParsedTransaction) | Task 2 |
| Channel detection from title (4 types + null for unknown) | Task 2 |
| Merchant from text before THB amount | Task 2 |
| Date from timestamp in Asia/Bangkok timezone | Task 2 |
| referenceNo always empty | Task 2 |
| Composite key dedup (merchant\|amount\|dateIso) | Tasks 3+4 |
| Room migration 2→3 for processed_refs schema | Task 3 |
| BankNotificationService reads EXTRA_TITLE + EXTRA_TEXT | Task 5 |
| BankNotificationService calls repository directly (no WorkManager for notification path) | Task 5 |
| Remove gmail.readonly OAuth scope | Task 6 |
| MerchantNormalizer stays accessible to CategoryResolver | Task 1 |

**All spec sections covered.**

**Type consistency check:** `NotificationParser.parse` returns `ParsedTransaction?` — matches what `TransactionRepository.processNewNotification` accepts. `ProcessedRefEntity(compositeKey)` used in both Task 3 (entity definition) and Task 4 (repository). `ProcessedRefDao.exists(key: String)` matches call site in Task 4 (`processedRefDao.exists(compositeKey)`). All consistent.

**No placeholders found.**
