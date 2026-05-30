package com.fidriyanto.banktracker.data.datasource

import com.fidriyanto.banktracker.data.db.ProcessedRefEntity
import com.fidriyanto.banktracker.data.db.TransactionEntity
import com.fidriyanto.banktracker.data.model.TransactionStatus
import com.fidriyanto.banktracker.domain.model.MerchantTotal
import kotlinx.coroutines.flow.Flow

interface TransactionLocalDataSource {
    fun observeAll(): Flow<List<TransactionEntity>>
    fun observeFiltered(month: String?, wallet: String?, txType: String?): Flow<List<TransactionEntity>>
    fun observeTransportTotalsTHB(category: String, fromDate: String, toDate: String): Flow<List<MerchantTotal>>
    fun observeTransportTotalsIDR(category: String, fromDate: String, toDate: String): Flow<List<MerchantTotal>>
    suspend fun getByStatus(status: TransactionStatus): List<TransactionEntity>
    suspend fun getById(id: Long): TransactionEntity?
    suspend fun insert(entity: TransactionEntity): Long
    suspend fun update(entity: TransactionEntity)
    suspend fun updateStatus(id: Long, status: TransactionStatus)
    suspend fun markAllSynced()
    suspend fun refExists(key: String): Int
    suspend fun insertRef(entity: ProcessedRefEntity)
}
