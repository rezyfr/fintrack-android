package com.fidriyanto.banktracker.data.datasource

import com.fidriyanto.banktracker.data.db.AutoAddedEntity
import kotlinx.coroutines.flow.Flow

interface AutoAddedDataSource {
    suspend fun add(row: AutoAddedEntity)
    fun observeActive(): Flow<List<AutoAddedEntity>>
    suspend fun dismissAll()
    suspend fun prune(cutoffMs: Long)
}
