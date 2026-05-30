package com.fidriyanto.banktracker.data.repository

import com.fidriyanto.banktracker.data.datasource.MerchantHistoryDataSource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MerchantHistoryRepositoryImpl @Inject constructor(
    private val dataSource: MerchantHistoryDataSource,
) : MerchantHistoryRepository {
    override fun observe(): Flow<List<String>> = dataSource.observe()
    override suspend fun save(merchant: String) = dataSource.save(merchant)
}
