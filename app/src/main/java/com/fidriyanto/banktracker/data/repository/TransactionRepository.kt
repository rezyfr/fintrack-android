package com.fidriyanto.banktracker.data.repository

import com.fidriyanto.banktracker.data.db.TransactionEntity
import com.fidriyanto.banktracker.data.model.ParsedTransaction
import com.fidriyanto.banktracker.data.model.TransactionEntry
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun observeTransactions(): Flow<List<TransactionEntity>>
    fun observeFiltered(month: String?, wallet: String?, txType: String?): Flow<List<TransactionEntity>>
    fun observePending(): Flow<List<TransactionEntity>>
    suspend fun processNewNotification(parsed: ParsedTransaction): Long?
    suspend fun syncTransaction(id: Long): Result<Unit>
    suspend fun insertManual(entry: TransactionEntry): Result<Unit>
    suspend fun updateAndSync(id: Long, item: String, category: String)
    suspend fun retryFailedSyncs()
    suspend fun markAllSynced()
}
