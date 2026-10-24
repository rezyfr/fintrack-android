package com.fidriyanto.banktracker.data.datasource

import com.fidriyanto.banktracker.domain.model.CustomBankPattern
import kotlinx.coroutines.flow.Flow

interface CustomBankDataSource {
    fun observe(): Flow<List<CustomBankPattern>>
    suspend fun all(): List<CustomBankPattern>
    suspend fun upsert(pattern: CustomBankPattern)
    suspend fun delete(id: String)
}
