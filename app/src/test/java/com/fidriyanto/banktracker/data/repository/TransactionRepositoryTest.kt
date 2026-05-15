package com.fidriyanto.banktracker.data.repository

import com.fidriyanto.banktracker.data.db.*
import com.fidriyanto.banktracker.data.model.*
import com.fidriyanto.banktracker.categorization.CategoryResolver
import com.fidriyanto.banktracker.email.EmailFetcher
import com.fidriyanto.banktracker.email.EmailParser
import com.fidriyanto.banktracker.sheets.SheetsSyncer
import com.fidriyanto.banktracker.data.prefs.SecurePrefs
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class TransactionRepositoryTest {

    private val transactionDao = mockk<TransactionDao>(relaxed = true)
    private val processedRefDao = mockk<ProcessedRefDao>(relaxed = true)
    private val emailFetcher = mockk<EmailFetcher>()
    private val emailParser = mockk<EmailParser>()
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
        referenceNo = "REF001",
        tab = SheetTab.EXPENSES,
        status = TransactionStatus.PENDING_EDIT
    )

    @Before
    fun setUp() {
        repository = TransactionRepository(
            transactionDao, processedRefDao,
            emailFetcher, emailParser, categoryResolver, sheetsSyncer, prefs
        )
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
