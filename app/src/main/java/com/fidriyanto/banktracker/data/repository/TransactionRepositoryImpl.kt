package com.fidriyanto.banktracker.data.repository

import com.fidriyanto.banktracker.data.datasource.AccountLocalDataSource
import com.fidriyanto.banktracker.data.datasource.CurrencySettingsDataSource
import com.fidriyanto.banktracker.data.datasource.InstallmentDataSource
import com.fidriyanto.banktracker.data.datasource.PendingInstallmentStepDataSource
import com.fidriyanto.banktracker.data.datasource.TransactionFetchDataSource
import com.fidriyanto.banktracker.data.datasource.TransactionLocalDataSource
import com.fidriyanto.banktracker.data.datasource.TransactionSyncDataSource
import com.fidriyanto.banktracker.data.datasource.remote.mapper.REMOTE_ID_OFFSET
import com.fidriyanto.banktracker.data.db.ProcessedRefEntity
import com.fidriyanto.banktracker.data.db.TransactionEntity
import com.fidriyanto.banktracker.data.model.ParsedTransaction
import com.fidriyanto.banktracker.data.model.TransactionEdit
import com.fidriyanto.banktracker.data.model.TransactionEntry
import com.fidriyanto.banktracker.data.model.TransactionStatus
import com.fidriyanto.banktracker.data.model.ledgerTabFor
import com.fidriyanto.banktracker.notification.notificationKeys
import com.fidriyanto.banktracker.domain.model.TransactionUiModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val localDataSource: TransactionLocalDataSource,
    private val syncDataSource: TransactionSyncDataSource,
    private val fetchDataSource: TransactionFetchDataSource,
    installmentDataSource: InstallmentDataSource,
    pendingStepDataSource: PendingInstallmentStepDataSource,
    private val accountDataSource: AccountLocalDataSource,
    private val currencySettingsDataSource: CurrencySettingsDataSource,
) : TransactionRepository {
    private val linker = InstallmentLinker(installmentDataSource, pendingStepDataSource)

    override fun observePending(): Flow<List<TransactionUiModel>> =
        localDataSource.observeAll().map { list ->
            list.filter { it.status != TransactionStatus.SYNCED }.map { it.toUiModel() }
        }

    override suspend fun fetch(
        month: String?, wallet: String?, txType: String?, category: String?,
        search: String?, amountMin: Double?, amountMax: Double?,
        dateFrom: String?, dateTo: String?, involvingWallet: String?,
    ): Result<List<TransactionUiModel>> =
        fetchDataSource.fetch(month, wallet, txType, category, search, amountMin, amountMax, dateFrom, dateTo, involvingWallet)
            .map { list -> list.map { it.toUiModel() } }

    override suspend fun processNewNotification(parsed: ParsedTransaction, alsoSeenAt: Long?): Long? {
        val keys = notificationKeys(parsed.item, parsed.amount, parsed.date, parsed.timestampMs, alsoSeenAt)
        // Atomic dedup: a ref insert ignores conflicts and returns -1 when the key exists. A check-then-insert raced
        // and recorded a transaction twice. Every key is inserted, so a later copy is caught under any of them.
        val fresh = keys.map { localDataSource.insertRef(ProcessedRefEntity(it)) != -1L }
        if (fresh.any { !it }) return null
        // ac: bca-expense-notification, configurable-currency-pair — the tab comes from the wallet's currency
        val settings = currencySettingsDataSource.observe().first()
        val walletCurrency = accountDataSource.observeAll().first().firstOrNull { it.id == parsed.wallet }?.currency
            ?: settings.homeCode
        val tab = ledgerTabFor(walletCurrency, settings.homeCode, isIncome = parsed.txType == "income")
        val entity = TransactionEntity(
            item = parsed.item, amount = parsed.amount, category = parsed.category,
            dateIso = parsed.date.toString(), referenceNo = "", tab = tab,
            status = TransactionStatus.PENDING_SYNC, wallet = parsed.wallet, txType = parsed.txType, subcategory = parsed.subcategory,
        )
        val id = localDataSource.insert(entity)
        syncTransaction(id)
        return id
    }

    override suspend fun syncTransaction(id: Long): Result<Unit> {
        val entity = localDataSource.getById(id)
            ?: return Result.failure(Exception("Transaction not found"))
        localDataSource.updateStatus(id, TransactionStatus.PENDING_SYNC)
        return linker.syncNew(entity.toEntry(), syncDataSource).also { result ->
            val newStatus = if (result.isSuccess) TransactionStatus.SYNCED else TransactionStatus.SYNC_FAILED
            localDataSource.updateStatus(id, newStatus)
        }
    }

    override suspend fun updateCategoryMultiple(ids: Set<Long>, category: String): Result<Unit> =
        batchUpdateCategory(ids, category, localDataSource, syncDataSource, ::syncTransaction)

    override suspend fun insertManual(entry: TransactionEntry): Result<Unit> {
        // Carry every field; wallet/txType/toWallet were previously dropped, so manual transfers and
        // income synced with wallet=null / txType=expense. ac: add-transfer-target-amount
        val entity = TransactionEntity(
            item = entry.item, amount = entry.amount, category = entry.category,
            dateIso = entry.date.toString(), referenceNo = "", tab = entry.tab,
            status = TransactionStatus.PENDING_SYNC, wallet = entry.wallet, txType = entry.txType,
            subcategory = entry.subcategory, toWallet = entry.toWallet, toAmount = entry.toAmount,
        ).also { it.installmentId = entry.installmentId }
        val id = localDataSource.insert(entity)
        return syncTransaction(id)
    }

    override suspend fun updateAndSync(id: Long, item: String, category: String) {
        val entity = localDataSource.getById(id) ?: return
        localDataSource.update(entity.copy(item = item, category = category))
        syncTransaction(id)
    }

    override suspend fun editTransaction(id: Long, edit: TransactionEdit): Result<Unit> {
        val remoteId = if (id >= REMOTE_ID_OFFSET) id - REMOTE_ID_OFFSET else id
        // ac: edit-transaction-from-feed — snapshot for restore if the PATCH fails
        val snapshot = localDataSource.getById(id)
        if (snapshot == null) return linker.updateWithLink(remoteId, edit, syncDataSource)
        // ac: edit-transaction-from-feed — optimistic local update before the remote PATCH resolves
        localDataSource.update(
            snapshot.copy(
                amount = edit.amount, item = edit.item, category = edit.category, dateIso = edit.dateIso, wallet = edit.wallet,
                txType = edit.txType, toWallet = edit.toWallet, toAmount = edit.toAmount, subcategory = edit.subcategory,
            )
        )
        return linker.updateWithLink(remoteId, edit, syncDataSource).onSuccess {
            // ac: installment-link-edit — keep the link on the local row, so the edit sheet shows it without a reload
            val link = if (edit.unlinkInstallment) null else edit.installmentId ?: snapshot.installmentId
            if (link != snapshot.installmentId) localDataSource.getById(id)?.let { localDataSource.update(it.copy().also { row -> row.installmentId = link }) }
        }.onFailure {
            // ac: edit-transaction-from-feed — restore the previous row on remote failure so the snackbar retry can re-attempt
            localDataSource.update(snapshot)
        }
    }

    override suspend fun deleteTransaction(id: Long): Result<Unit> {
        val remoteId = if (id >= REMOTE_ID_OFFSET) id - REMOTE_ID_OFFSET else id
        // ac: delete-transaction-from-feed — snapshot for restore on remote failure
        val snapshot = localDataSource.getById(id)
        if (snapshot == null) return syncDataSource.delete(remoteId)
        // ac: delete-transaction-from-feed — optimistic local removal before the remote call
        localDataSource.deleteById(id)
        return syncDataSource.delete(remoteId).onFailure {
            // ac: delete-transaction-from-feed — restore on remote failure so the snackbar retry can re-attempt
            localDataSource.insert(snapshot)
        }
    }

    override suspend fun retryFailedSyncs() {
        linker.flushPending()
        localDataSource.getByStatus(TransactionStatus.SYNC_FAILED).forEach { syncTransaction(it.id) }
        localDataSource.getByStatus(TransactionStatus.PENDING_SYNC).forEach { syncTransaction(it.id) }
    }

    override suspend fun markAllSynced() = localDataSource.markAllSynced()

    override suspend fun recentTransferRates(fromWallets: List<String>, toWallets: List<String>): Result<List<Double>> =
        fetchDataSource.recentTransferRates(fromWallets, toWallets)
}
