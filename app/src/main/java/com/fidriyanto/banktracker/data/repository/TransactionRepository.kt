package com.fidriyanto.banktracker.data.repository

import com.fidriyanto.banktracker.categorization.CategoryResolver
import com.fidriyanto.banktracker.data.db.*
import com.fidriyanto.banktracker.data.model.*
import com.fidriyanto.banktracker.data.prefs.SecurePrefs
import com.fidriyanto.banktracker.email.EmailFetcher
import com.fidriyanto.banktracker.email.EmailParser
import com.fidriyanto.banktracker.sheets.SheetsSyncer
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    private val processedRefDao: ProcessedRefDao,
    private val emailFetcher: EmailFetcher,
    private val emailParser: EmailParser,
    private val categoryResolver: CategoryResolver,
    private val sheetsSyncer: SheetsSyncer,
    private val prefs: SecurePrefs
) {
    fun observeTransactions(): Flow<List<TransactionEntity>> = transactionDao.observeAll()

    suspend fun processNewNotification(triggerAmount: Double): Long? {
        val html = emailFetcher.fetchLatestBankEmail() ?: return null
        val parsed = emailParser.parse(html) ?: return null

        if (parsed.referenceNo.isNotEmpty() && processedRefDao.exists(parsed.referenceNo) > 0) return null
        if (parsed.referenceNo.isNotEmpty()) {
            processedRefDao.insert(ProcessedRefEntity(parsed.referenceNo))
        }

        val resolved = categoryResolver.resolve(parsed, prefs.promptPayThreshold, prefs.claudeApiKey)

        val entity = TransactionEntity(
            merchant = parsed.merchant,
            item = resolved.description,
            amount = parsed.amount,
            category = resolved.category,
            dateIso = parsed.date.toString(),
            channel = parsed.channel,
            referenceNo = parsed.referenceNo,
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

    suspend fun retryFailedSyncs() {
        transactionDao.getByStatus(TransactionStatus.SYNC_FAILED).forEach { syncTransaction(it.id) }
        transactionDao.getByStatus(TransactionStatus.PENDING_SYNC).forEach { syncTransaction(it.id) }
    }
}
