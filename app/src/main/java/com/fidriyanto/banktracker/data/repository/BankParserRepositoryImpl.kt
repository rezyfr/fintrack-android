package com.fidriyanto.banktracker.data.repository

import com.fidriyanto.banktracker.data.datasource.BankParserDataSource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BankParserRepositoryImpl @Inject constructor(
    private val local: BankParserDataSource,
) : BankParserRepository {
    override fun observe(): Flow<Map<String, String>> = local.observe()
    override suspend fun save(bankId: String, accountId: String) = local.save(bankId, accountId)
}
