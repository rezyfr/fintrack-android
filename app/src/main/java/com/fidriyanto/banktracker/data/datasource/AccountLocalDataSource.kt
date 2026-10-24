package com.fidriyanto.banktracker.data.datasource

import com.fidriyanto.banktracker.data.db.AccountEntity
import kotlinx.coroutines.flow.Flow

interface AccountLocalDataSource {
    fun observeAll(): Flow<List<AccountEntity>>
    suspend fun replaceAll(rows: List<AccountEntity>)
    suspend fun upsertOne(row: AccountEntity)
    suspend fun delete(id: String)
}
