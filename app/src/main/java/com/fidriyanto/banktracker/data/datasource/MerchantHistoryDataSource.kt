package com.fidriyanto.banktracker.data.datasource

import kotlinx.coroutines.flow.Flow

interface MerchantHistoryDataSource {
    fun observe(): Flow<List<String>>
    suspend fun save(merchant: String)
}
