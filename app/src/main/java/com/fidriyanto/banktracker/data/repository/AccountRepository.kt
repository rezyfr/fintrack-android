package com.fidriyanto.banktracker.data.repository

import com.fidriyanto.banktracker.domain.model.Account
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun observeAll(): Flow<List<Account>>
    suspend fun refresh(): Result<Unit>
    suspend fun save(account: Account): Result<Unit>
    suspend fun delete(id: String): Result<Unit>
}
