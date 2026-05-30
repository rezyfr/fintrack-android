package com.fidriyanto.banktracker.data.repository

import com.fidriyanto.banktracker.data.datasource.TransactionFetchDataSource
import com.fidriyanto.banktracker.data.db.TransactionEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionFetchRepositoryImpl @Inject constructor(
    private val dataSource: TransactionFetchDataSource
) : TransactionFetchRepository {
    override suspend fun fetch(month: String?, wallet: String?, txType: String?): Result<List<TransactionEntity>> =
        dataSource.fetch(month, wallet, txType)
}
