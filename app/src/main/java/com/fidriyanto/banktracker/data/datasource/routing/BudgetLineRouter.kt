package com.fidriyanto.banktracker.data.datasource.routing

import com.fidriyanto.banktracker.data.datasource.StorageSettingsDataSource
import com.fidriyanto.banktracker.data.datasource.offline.OfflineBudgetLineDataSource
import com.fidriyanto.banktracker.data.datasource.remote.BudgetLineRemoteDataSource
import com.fidriyanto.banktracker.data.datasource.remote.BudgetLineRemoteDataSourceImpl
import com.fidriyanto.banktracker.data.datasource.remote.dto.BudgetLineDto
import com.fidriyanto.banktracker.domain.model.BudgetLine
import javax.inject.Inject
import javax.inject.Singleton

// ac: offline-mode - sends each call to BudgetLineRemoteDataSourceImpl or OfflineBudgetLineDataSource, by the storage the user chose.
@Singleton
class BudgetLineRouter @Inject constructor(
    storage: StorageSettingsDataSource,
    server: BudgetLineRemoteDataSourceImpl,
    offline: OfflineBudgetLineDataSource,
) : BudgetLineRemoteDataSource {
    private val pick = StorageSwitch<BudgetLineRemoteDataSource>(storage, server, offline)

    override suspend fun getBudgetLines(): List<BudgetLineDto> = pick().getBudgetLines()

    override suspend fun create(line: BudgetLine): Result<Unit> = pick().create(line)

    override suspend fun update(line: BudgetLine): Result<Unit> = pick().update(line)

    override suspend fun delete(id: Long): Result<Unit> = pick().delete(id)
}
