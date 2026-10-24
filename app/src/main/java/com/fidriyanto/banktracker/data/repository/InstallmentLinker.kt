package com.fidriyanto.banktracker.data.repository

import android.util.Log
import com.fidriyanto.banktracker.data.datasource.InstallmentDataSource
import com.fidriyanto.banktracker.data.datasource.PendingInstallmentStepDataSource
import com.fidriyanto.banktracker.data.datasource.TransactionSyncDataSource
import com.fidriyanto.banktracker.data.datasource.remote.mapper.toDomain
import com.fidriyanto.banktracker.data.model.TransactionEdit
import com.fidriyanto.banktracker.data.model.TransactionEntry
import com.fidriyanto.banktracker.domain.model.InstallmentMatch
import com.fidriyanto.banktracker.domain.model.InstallmentMatcher

private const val TAG = "InstallmentLinker"

// ac: installment-auto-link — links a card transaction to its installment and moves the installment
// to its new step. The transaction is sent with its installment_id. Once the transaction is saved,
// the step update is sent. A step update that fails is kept in a queue and sent again later.
internal class InstallmentLinker(
    private val installments: InstallmentDataSource,
    private val pendingSteps: PendingInstallmentStepDataSource,
) {
    // ac: installment-link-step-retry — sends the step updates that failed earlier. Each one leaves the
    // queue only after the server accepts it.
    suspend fun flushPending() {
        pendingSteps.getAll().forEach { row ->
            installments.incrementStep(row.installmentId, row.newStep, row.newStatus)
                .onSuccess { pendingSteps.delete(row.installmentId) }
                .onFailure { Log.w(TAG, "step update still waiting for installment ${row.installmentId}", it) }
        }
    }

    // ac: installment-link-step-retry — sends the step update, or queues it when the send fails
    suspend fun advance(match: InstallmentMatch) {
        if (match.newStep == match.installment.currentStep) return
        installments.incrementStep(match.installment.id, match.newStep, match.newStatus).onFailure {
            Log.w(TAG, "step update queued for installment ${match.installment.id}", it)
            pendingSteps.save(match.installment.id, match.newStep, match.newStatus)
        }
    }

    // ac: installment-auto-link — a new expense on a wallet is matched before it is sent
    suspend fun syncNew(entry: TransactionEntry, syncDataSource: TransactionSyncDataSource): Result<Unit> {
        flushPending()
        // ac: schedule-installment-link - a transaction that already names its plan moves that plan up one step.
        // When the plans cannot be read, the save fails and is tried again later, so the step is not lost.
        val match = if (entry.installmentId != null) {
            val plan = (installmentsOrNull() ?: return Result.failure(Exception("installments unavailable")))
                .firstOrNull { it.id == entry.installmentId }
            plan?.let { InstallmentMatcher.advance(it, entry.item, strict = false) }
        } else findMatch(entry.item, entry.wallet, entry.amount, entry.txType)
        val result = syncDataSource.sync(entry.copy(installmentId = match?.installment?.id))
        if (result.isSuccess && match != null) advance(match)
        return result
    }

    // ac: installment-link-edit — saves an edit. An installment picked by hand is linked, an unlinked
    // card expense is matched automatically, and an unlink sends its own PATCH after the save.
    suspend fun updateWithLink(remoteId: Long, edit: TransactionEdit, syncDataSource: TransactionSyncDataSource): Result<Unit> {
        val match = when {
            edit.installmentId != null ->
                installmentsOrNull()?.firstOrNull { it.id == edit.installmentId }
                    ?.let { InstallmentMatcher.advance(it, edit.item, strict = false) }
            edit.autoLinkInstallment -> findMatch(edit.item, edit.wallet, edit.amount, edit.txType)
            else -> null
        }
        val result = syncDataSource.update(remoteId, if (match == null) edit else edit.copy(installmentId = match.installment.id))
        if (result.isSuccess) {
            if (edit.unlinkInstallment) syncDataSource.unlinkInstallment(remoteId)
            if (match != null) advance(match)
        }
        return result
    }

    private suspend fun installmentsOrNull() = installments.fetchAll().getOrNull()?.map { it.toDomain() }

    private suspend fun findMatch(item: String, wallet: String?, amount: Double, txType: String): InstallmentMatch? {
        if (wallet == null || txType != "expense") return null
        return installmentsOrNull()?.let { InstallmentMatcher.match(item, wallet, amount, it) }
    }
}
