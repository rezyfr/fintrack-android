package com.fidriyanto.banktracker.data.datasource

import com.fidriyanto.banktracker.data.db.ProcessedRefDao
import com.fidriyanto.banktracker.data.db.ProcessedRefEntity
import com.fidriyanto.banktracker.data.db.TransactionDao
import com.fidriyanto.banktracker.data.db.TransactionEntity
import com.fidriyanto.banktracker.data.model.TransactionStatus
import com.fidriyanto.banktracker.domain.model.MerchantTotal
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionLocalDataSourceImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val processedRefDao: ProcessedRefDao
) : TransactionLocalDataSource {
    override fun observeAll() = transactionDao.observeAll()
    override fun observeFiltered(month: String?, wallet: String?, txType: String?) =
        transactionDao.observeFiltered(month, wallet, txType)
    override fun observeTransportTotalsTHB(category: String, fromDate: String, toDate: String): Flow<List<MerchantTotal>> =
        transactionDao.observeTransportTotalsTHB(category, fromDate, toDate)
    override fun observeTransportTotalsIDR(category: String, fromDate: String, toDate: String): Flow<List<MerchantTotal>> =
        transactionDao.observeTransportTotalsIDR(category, fromDate, toDate)
    override suspend fun getByStatus(status: TransactionStatus) = transactionDao.getByStatus(status)
    override suspend fun getById(id: Long) = transactionDao.getById(id)
    override suspend fun insert(entity: TransactionEntity) = transactionDao.insert(entity)
    override suspend fun update(entity: TransactionEntity) = transactionDao.update(entity)
    override suspend fun updateStatus(id: Long, status: TransactionStatus) =
        transactionDao.updateStatus(id, status)
    override suspend fun deleteById(id: Long) = transactionDao.deleteById(id)
    override suspend fun markAllSynced() = transactionDao.markAllSynced()
    override suspend fun refExists(key: String) = processedRefDao.exists(key)
    override suspend fun insertRef(entity: ProcessedRefEntity): Long = processedRefDao.insert(entity)
}
