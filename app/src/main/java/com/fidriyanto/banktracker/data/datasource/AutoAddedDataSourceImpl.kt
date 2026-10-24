package com.fidriyanto.banktracker.data.datasource

import com.fidriyanto.banktracker.data.db.AutoAddedDao
import com.fidriyanto.banktracker.data.db.AutoAddedEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutoAddedDataSourceImpl @Inject constructor(
    private val dao: AutoAddedDao,
) : AutoAddedDataSource {
    override suspend fun add(row: AutoAddedEntity) { dao.insert(row) }
    override fun observeActive(): Flow<List<AutoAddedEntity>> = dao.observeActive()
    override suspend fun dismissAll() = dao.dismissAll()
    override suspend fun prune(cutoffMs: Long) = dao.prune(cutoffMs)
}
