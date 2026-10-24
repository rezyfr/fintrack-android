package com.fidriyanto.banktracker.data.repository

import com.fidriyanto.banktracker.domain.model.AutoAddedItem
import com.fidriyanto.banktracker.domain.model.AutoSource
import kotlinx.coroutines.flow.Flow

interface AutoAddedRepository {
    suspend fun add(item: String, amount: Double, txType: String, wallet: String?, source: AutoSource)
    fun observeActive(): Flow<List<AutoAddedItem>>
    suspend fun dismissAll()
    suspend fun prune(cutoffMs: Long)
}
