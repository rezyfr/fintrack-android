package com.fidriyanto.banktracker.data.repository

import kotlinx.coroutines.flow.Flow

interface BankParserRepository {
    fun observe(): Flow<Map<String, String>>
    suspend fun save(bankId: String, accountId: String)
}
