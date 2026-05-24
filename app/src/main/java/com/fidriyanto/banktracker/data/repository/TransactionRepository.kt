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
            merchant = entity.merchant,
            item = entity.item,
            amount = entity.amount,
            category = entity.category,
            channel = entity.channel
        )
        return sheetsSyncer.sync(row).also { result ->
            val newStatus = if (result.isSuccess) TransactionStatus.SYNCED else TransactionStatus.SYNC_FAILED
            transactionDao.updateStatus(id, newStatus)
        }
    }

    suspend fun insertManual(row: SheetsRow): Result<Unit> {
        val entity = TransactionEntity(
            merchant = row.merchant,
            item = row.item,
            amount = row.amount,
            category = row.category,
            dateIso = row.date.toString(),
            channel = row.channel,
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
