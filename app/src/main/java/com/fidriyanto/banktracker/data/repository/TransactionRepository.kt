package com.fidriyanto.banktracker.data.repository

import com.fidriyanto.banktracker.data.model.ParsedTransaction
import com.fidriyanto.banktracker.data.model.TransactionEdit
import com.fidriyanto.banktracker.data.model.TransactionEntry
import com.fidriyanto.banktracker.domain.model.TransactionUiModel
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun observePending(): Flow<List<TransactionUiModel>>
    suspend fun fetch(
        month: String?, wallet: String?, txType: String?, category: String? = null,
        search: String? = null, amountMin: Double? = null, amountMax: Double? = null,
        dateFrom: String? = null, dateTo: String? = null,
    ): Result<List<TransactionUiModel>>
    suspend fun processNewNotification(parsed: ParsedTransaction): Long?
    suspend fun syncTransaction(id: Long): Result<Unit>
    suspend fun insertManual(entry: TransactionEntry): Result<Unit>
    suspend fun updateAndSync(id: Long, item: String, category: String)
    // ac: batch-edit-transaction-category — updates the category of every id in one call
    suspend fun updateCategoryMultiple(ids: Set<Long>, category: String): Result<Unit>
    suspend fun deleteTransaction(id: Long): Result<Unit>
    suspend fun editTransaction(id: Long, edit: TransactionEdit): Result<Unit>
    suspend fun retryFailedSyncs()
    suspend fun markAllSynced()
}
