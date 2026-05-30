package com.fidriyanto.banktracker.data.repository

import com.fidriyanto.banktracker.data.datasource.TransactionLocalDataSource
import com.fidriyanto.banktracker.data.datasource.TransactionSyncDataSource
import com.fidriyanto.banktracker.data.db.ProcessedRefEntity
import com.fidriyanto.banktracker.data.db.TransactionEntity
import com.fidriyanto.banktracker.data.model.LedgerTab
import com.fidriyanto.banktracker.data.model.ParsedTransaction
import com.fidriyanto.banktracker.data.model.TransactionEntry
import com.fidriyanto.banktracker.data.model.TransactionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val localDataSource: TransactionLocalDataSource,
    private val syncDataSource: TransactionSyncDataSource
) : TransactionRepository {

    override fun observeTransactions(): Flow<List<TransactionEntity>> = localDataSource.observeAll()

    override fun observeFiltered(month: String?, wallet: String?, txType: String?) =
        localDataSource.observeFiltered(month, wallet, txType)

    override fun observePending(): Flow<List<TransactionEntity>> =
        localDataSource.observeAll().map { list -> list.filter { it.status != TransactionStatus.SYNCED } }

    override suspend fun processNewNotification(parsed: ParsedTransaction): Long? {
        val compositeKey = "${parsed.merchant}|${parsed.amount}|${parsed.date}"
        if (localDataSource.refExists(compositeKey) > 0) return null
        localDataSource.insertRef(ProcessedRefEntity(compositeKey))
        val entity = TransactionEntity(
            merchant = parsed.merchant,
            item     = parsed.merchant,
            amount   = parsed.amount,
            category = "Other",
            dateIso  = parsed.date.toString(),
            referenceNo = "",
            tab      = LedgerTab.EXPENSES,
            status   = TransactionStatus.PENDING_SYNC
        )
        val id = localDataSource.insert(entity)
        syncTransaction(id)
        return id
    }

    override suspend fun syncTransaction(id: Long): Result<Unit> {
        val entity = localDataSource.getById(id)
            ?: return Result.failure(Exception("Transaction not found"))
        localDataSource.updateStatus(id, TransactionStatus.PENDING_SYNC)
        val entry = TransactionEntry(
            tab      = entity.tab,
            date     = LocalDate.parse(entity.dateIso),
            merchant = entity.merchant,
            item     = entity.item,
            amount   = entity.amount,
            category = entity.category,
        )
        return syncDataSource.sync(entry).also { result ->
            val newStatus = if (result.isSuccess) TransactionStatus.SYNCED else TransactionStatus.SYNC_FAILED
            localDataSource.updateStatus(id, newStatus)
        }
    }

    override suspend fun insertManual(entry: TransactionEntry): Result<Unit> {
        val entity = TransactionEntity(
            merchant = entry.merchant,
            item     = entry.item,
            amount   = entry.amount,
            category = entry.category,
            dateIso  = entry.date.toString(),
            referenceNo = "",
            tab      = entry.tab,
            status   = TransactionStatus.PENDING_SYNC
        )
        val id = localDataSource.insert(entity)
        return syncTransaction(id)
    }

    override suspend fun updateAndSync(id: Long, item: String, category: String) {
        val entity = localDataSource.getById(id) ?: return
        localDataSource.update(entity.copy(item = item, category = category))
        syncTransaction(id)
    }

    override suspend fun retryFailedSyncs() {
        localDataSource.getByStatus(TransactionStatus.SYNC_FAILED).forEach { syncTransaction(it.id) }
        localDataSource.getByStatus(TransactionStatus.PENDING_SYNC).forEach { syncTransaction(it.id) }
    }

    override suspend fun markAllSynced() = localDataSource.markAllSynced()
}
