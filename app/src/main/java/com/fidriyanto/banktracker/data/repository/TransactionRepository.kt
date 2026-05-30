package com.fidriyanto.banktracker.data.repository

import com.fidriyanto.banktracker.data.model.ParsedTransaction
import com.fidriyanto.banktracker.data.model.TransactionEntry
import com.fidriyanto.banktracker.domain.model.TransactionUiModel
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun observePending(): Flow<List<TransactionUiModel>>
    suspend fun fetch(month: String?, wallet: String?, txType: String?): Result<List<TransactionUiModel>>
    suspend fun processNewNotification(parsed: ParsedTransaction): Long?
    suspend fun syncTransaction(id: Long): Result<Unit>
    suspend fun insertManual(entry: TransactionEntry): Result<Unit>
    suspend fun updateAndSync(id: Long, item: String, category: String)
    suspend fun retryFailedSyncs()
    suspend fun markAllSynced()
}
