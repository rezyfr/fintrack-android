package com.fidriyanto.banktracker.data.datasource

import kotlinx.coroutines.flow.Flow

interface BankParserDataSource {
    // Bank id to the account id the user chose. An empty text means the user turned that bank off.
    // A bank with no entry was never set.
    fun observe(): Flow<Map<String, String>>
    suspend fun save(bankId: String, accountId: String)
    // Removes every entry, then writes the given ones. A backup import uses it.
    suspend fun replaceAll(entries: Map<String, String>)
}
