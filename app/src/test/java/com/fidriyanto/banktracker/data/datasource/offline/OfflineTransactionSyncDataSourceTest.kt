package com.fidriyanto.banktracker.data.datasource.offline

import com.fidriyanto.banktracker.data.db.LedgerDao
import com.fidriyanto.banktracker.data.db.LedgerTransactionEntity
import com.fidriyanto.banktracker.data.model.LedgerTab
import com.fidriyanto.banktracker.data.model.TransactionEdit
import com.fidriyanto.banktracker.data.model.TransactionEntry
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class OfflineTransactionSyncDataSourceTest {
    private val dao = mockk<LedgerDao>(relaxed = true)
    private val source = OfflineTransactionSyncDataSource(dao)
    private val row = LedgerTransactionEntity(
        id = 7, item = "Lunch", amount = 10.0, category = "Food", subcategory = "Cafe", date = "2026-10-01",
        tab = "EXPENSES", wallet = "A", txType = "transfer", toWallet = "B", toAmount = 9.0, installmentId = 3,
    )
    private val entry = TransactionEntry(LedgerTab.EXPENSES, LocalDate.of(2026, 10, 2), "Coffee", 4.0, "Food", wallet = "A")

    @Test fun aNewTransactionNeedsAnAccount() = runBlocking {
        assertTrue(source.sync(entry.copy(wallet = null)).isFailure)
        coVerify(exactly = 0) { dao.insertTransaction(any()) }
    }

    @Test fun aNewTransactionIsWrittenWithItsDate() = runBlocking {
        val saved = slot<LedgerTransactionEntity>()
        coEvery { dao.insertTransaction(capture(saved)) } returns 1L
        assertTrue(source.sync(entry).isSuccess)
        assertEquals("2026-10-02", saved.captured.date)
        assertEquals("A", saved.captured.wallet)
    }

    @Test fun aLinkToAMissingInstallmentFails() = runBlocking {
        coEvery { dao.installment(9) } returns null
        assertTrue(source.sync(entry.copy(installmentId = 9)).isFailure)
    }

    @Test fun anEditKeepsTheFieldsItLeavesEmpty() = runBlocking {
        coEvery { dao.transaction(7) } returns row
        val saved = slot<LedgerTransactionEntity>()
        coEvery { dao.updateTransaction(capture(saved)) } returns Unit
        val edit = TransactionEdit(amount = 12.0, item = "Dinner", category = "Food", dateIso = "2026-10-03", wallet = null, txType = "transfer", toWallet = null)
        assertTrue(source.update(7, edit).isSuccess)
        assertEquals(row.copy(amount = 12.0, item = "Dinner", date = "2026-10-03"), saved.captured)
    }

    @Test fun anEditOfAMissingRowFails() = runBlocking {
        coEvery { dao.transaction(8) } returns null
        val edit = TransactionEdit(1.0, "x", "Food", "2026-10-03", null, "expense", null)
        assertTrue(source.update(8, edit).isFailure)
    }
}
