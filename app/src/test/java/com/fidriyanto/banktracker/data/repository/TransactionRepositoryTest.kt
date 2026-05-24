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
        coVerify { processedRefDao.insert(match { it.compositeKey == compositeKey }) }
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
