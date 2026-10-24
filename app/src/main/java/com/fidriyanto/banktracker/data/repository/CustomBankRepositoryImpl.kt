package com.fidriyanto.banktracker.data.repository

import com.fidriyanto.banktracker.data.datasource.CustomBankDataSource
import com.fidriyanto.banktracker.domain.model.CustomBankPattern
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomBankRepositoryImpl @Inject constructor(
    private val local: CustomBankDataSource,
) : CustomBankRepository {
    override fun observe(): Flow<List<CustomBankPattern>> = local.observe()
    override suspend fun all(): List<CustomBankPattern> = local.all()
    override suspend fun upsert(pattern: CustomBankPattern) = local.upsert(pattern)
    override suspend fun delete(id: String) = local.delete(id)
}
