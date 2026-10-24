package com.fidriyanto.banktracker.data.repository

import com.fidriyanto.banktracker.data.datasource.InstallmentDataSource
import com.fidriyanto.banktracker.data.datasource.PendingInstallmentStepDataSource
import com.fidriyanto.banktracker.data.datasource.TransactionSyncDataSource
import com.fidriyanto.banktracker.data.datasource.remote.dto.InstallmentCreateDto
import com.fidriyanto.banktracker.data.datasource.remote.dto.InstallmentDto
import com.fidriyanto.banktracker.data.db.PendingInstallmentStepEntity
import com.fidriyanto.banktracker.data.model.LedgerTab
import com.fidriyanto.banktracker.data.model.TransactionEdit
import com.fidriyanto.banktracker.data.model.TransactionEntry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

private class FakeInstallments(var plans: List<InstallmentDto>, var failStep: Boolean = false) : InstallmentDataSource {
    val stepCalls = mutableListOf<Triple<Long, Int, String>>()
    override suspend fun fetchAll() = Result.success(plans)
    override suspend fun updateMerchant(id: Long, merchant: String) = Result.success(Unit)
    override suspend fun incrementStep(id: Long, newStep: Int, newStatus: String): Result<Unit> {
        if (failStep) return Result.failure(Exception("offline"))
        stepCalls += Triple(id, newStep, newStatus)
        return Result.success(Unit)
    }
    override suspend fun setExcluded(id: Long, excluded: Boolean) = Result.success(Unit)
    override suspend fun create(body: InstallmentCreateDto): Result<InstallmentDto> = Result.failure(Exception("not used"))
    override suspend fun delete(id: Long) = Result.success(Unit)
}

private class FakePending : PendingInstallmentStepDataSource {
    val rows = mutableMapOf<Long, PendingInstallmentStepEntity>()
    override suspend fun getAll() = rows.values.toList()
    override suspend fun save(installmentId: Long, newStep: Int, newStatus: String) {
        rows[installmentId] = PendingInstallmentStepEntity(installmentId, newStep, newStatus)
    }
    override suspend fun delete(installmentId: Long) { rows.remove(installmentId) }
}

private class FakeSync : TransactionSyncDataSource {
    var synced: TransactionEntry? = null
    var updated: TransactionEdit? = null
    var unlinked = false
    override suspend fun sync(entry: TransactionEntry): Result<Unit> { synced = entry; return Result.success(Unit) }
    override suspend fun delete(id: Long) = Result.success(Unit)
    override suspend fun update(id: Long, edit: TransactionEdit): Result<Unit> { updated = edit; return Result.success(Unit) }
    override suspend fun unlinkInstallment(id: Long): Result<Unit> { unlinked = true; return Result.success(Unit) }
    override suspend fun updateCategoryBatch(remoteIds: List<Long>, category: String) = Result.success(Unit)
}

class InstallmentLinkerTest {
    private val tiketCom = InstallmentDto(
        id = 7, merchant = "Tiket.com", merchantPattern = "Tiket.com", wallet = "MANDIRI_CC", installmentAmount = 25920.0,
        totalInstallments = 12, currentStep = 9, dueDay = 30, status = "active",
    )

    private fun entry(item: String, amount: Double = 25920.0, wallet: String? = "MANDIRI_CC", type: String = "expense") =
        TransactionEntry(
            tab = LedgerTab.IDR_EXPENSES, date = LocalDate.of(2026, 9, 30), item = item, amount = amount,
            category = "Travel", wallet = wallet, txType = type,
        )

    private fun edit(item: String = "TIKET.COM J 010/012", amount: Double = 25920.0) = TransactionEdit(
        amount = amount, item = item, category = "Travel", dateIso = "2026-09-30", wallet = "MANDIRI_CC",
        txType = "expense", toWallet = null,
    )

    private fun linker(installments: FakeInstallments, pending: FakePending = FakePending()) = InstallmentLinker(installments, pending)

    @Test fun aMatchingExpenseIsSentWithItsInstallmentAndTheStepMovesUp() = runBlocking {
        val inst = FakeInstallments(listOf(tiketCom)); val sync = FakeSync()
        linker(inst).syncNew(entry("TIKET.COM J 010/012"), sync)
        assertEquals(7L, sync.synced?.installmentId)
        assertEquals(listOf(Triple(7L, 10, "active")), inst.stepCalls)
    }

    @Test fun aTransactionWithoutAMatchIsSentUnlinkedAndNoStepMoves() = runBlocking {
        val inst = FakeInstallments(listOf(tiketCom)); val sync = FakeSync()
        linker(inst).syncNew(entry("Groceries", amount = 500.0), sync)
        assertNull(sync.synced?.installmentId)
        assertTrue(inst.stepCalls.isEmpty())
    }

    @Test fun anIncomeTransactionIsNeverLinked() = runBlocking {
        val inst = FakeInstallments(listOf(tiketCom)); val sync = FakeSync()
        linker(inst).syncNew(entry("TIKET.COM J 010/012", type = "income"), sync)
        assertNull(sync.synced?.installmentId)
    }

    @Test fun aFailedStepUpdateIsQueuedAndTheTransactionStillSaves() = runBlocking {
        val inst = FakeInstallments(listOf(tiketCom), failStep = true); val sync = FakeSync(); val pending = FakePending()
        val result = linker(inst, pending).syncNew(entry("TIKET.COM J 010/012"), sync)
        assertTrue(result.isSuccess)
        assertEquals(7L, sync.synced?.installmentId)
        assertEquals(10, pending.rows[7L]?.newStep)
    }

    @Test fun aQueuedStepIsSentLaterAndLeavesTheQueue() = runBlocking {
        val inst = FakeInstallments(listOf(tiketCom)); val pending = FakePending()
        pending.save(7, 10, "active")
        linker(inst, pending).flushPending()
        assertEquals(listOf(Triple(7L, 10, "active")), inst.stepCalls)
        assertTrue(pending.rows.isEmpty())
    }

    @Test fun aQueuedStepStaysQueuedWhileTheSendKeepsFailing() = runBlocking {
        val inst = FakeInstallments(listOf(tiketCom), failStep = true); val pending = FakePending()
        pending.save(7, 10, "active")
        linker(inst, pending).flushPending()
        assertEquals(1, pending.rows.size)
    }

    @Test fun anEditOfAnUnlinkedExpenseLinksItWhenItMatches() = runBlocking {
        val inst = FakeInstallments(listOf(tiketCom)); val sync = FakeSync()
        linker(inst).updateWithLink(5, edit().copy(autoLinkInstallment = true), sync)
        assertEquals(7L, sync.updated?.installmentId)
        assertEquals(10, inst.stepCalls.single().second)
    }

    @Test fun anEditOfALinkedExpenseChangesNoStep() = runBlocking {
        val inst = FakeInstallments(listOf(tiketCom)); val sync = FakeSync()
        linker(inst).updateWithLink(5, edit(), sync)
        assertNull(sync.updated?.installmentId)
        assertTrue(inst.stepCalls.isEmpty())
    }

    @Test fun aPlanPickedByHandIsLinkedEvenWhenTheTextHasNoStep() = runBlocking {
        val inst = FakeInstallments(listOf(tiketCom)); val sync = FakeSync()
        linker(inst).updateWithLink(5, edit(item = "Something else").copy(installmentId = 7), sync)
        assertEquals(7L, sync.updated?.installmentId)
        assertEquals(10, inst.stepCalls.single().second)
    }

    @Test fun anUnlinkSendsItsOwnPatchAndMovesNoStep() = runBlocking {
        val inst = FakeInstallments(listOf(tiketCom)); val sync = FakeSync()
        linker(inst).updateWithLink(5, edit().copy(unlinkInstallment = true), sync)
        assertTrue(sync.unlinked)
        assertTrue(inst.stepCalls.isEmpty())
    }

    @Test fun anEntryThatNamesItsPlanIsSentWithTheLinkAndTheStepMovesUp() = runBlocking {
        val inst = FakeInstallments(listOf(tiketCom)); val sync = FakeSync()
        val result = linker(inst).syncNew(entry("Flight tickets").copy(installmentId = 7), sync)
        assertTrue(result.isSuccess)
        assertEquals(7L, sync.synced?.installmentId)
        assertEquals(listOf(Triple(7L, 10, "active")), inst.stepCalls)
    }

    @Test fun anEntryThatNamesAMissingPlanIsSentWithoutALink() = runBlocking {
        val inst = FakeInstallments(listOf(tiketCom)); val sync = FakeSync()
        linker(inst).syncNew(entry("Flight tickets").copy(installmentId = 99), sync)
        assertNull(sync.synced?.installmentId)
        assertTrue(inst.stepCalls.isEmpty())
    }
}
