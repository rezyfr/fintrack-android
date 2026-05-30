package com.fidriyanto.banktracker.data.repository

import kotlinx.coroutines.flow.Flow

interface MerchantHistoryRepository {
    fun observe(): Flow<List<String>>
    suspend fun save(merchant: String)
}
