package com.fidriyanto.banktracker.data.datasource

import com.fidriyanto.banktracker.data.db.AccountDao
import com.fidriyanto.banktracker.data.db.AccountEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountLocalDataSourceImpl @Inject constructor(
    private val dao: AccountDao,
) : AccountLocalDataSource {
    override fun observeAll(): Flow<List<AccountEntity>> = dao.observeAll()

    override suspend fun replaceAll(rows: List<AccountEntity>) {
        dao.upsertAll(rows)
        dao.deleteMissing(rows.map { it.id })
    }

    override suspend fun upsertOne(row: AccountEntity) = dao.upsertAll(listOf(row))

    override suspend fun delete(id: String) = dao.delete(id)
}
