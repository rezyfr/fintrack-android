package com.fidriyanto.banktracker.data.repository

import com.fidriyanto.banktracker.data.datasource.remote.BudgetLineRemoteDataSource
import com.fidriyanto.banktracker.domain.model.BudgetLine
import javax.inject.Inject

class BudgetLineRepositoryImpl @Inject constructor(
    private val remote: BudgetLineRemoteDataSource,
) : BudgetLineRepository {
    override suspend fun getAll(): Result<List<BudgetLine>> = runCatching {
        remote.getBudgetLines().map {
            BudgetLine(it.id, it.name, it.kind, it.currency, it.target,
                it.matchWallets, it.matchPattern, it.matchCategories, it.sortOrder, it.category, it.subcategory, it.dueDay)
        }
    }

    override suspend fun create(line: BudgetLine): Result<Unit> = remote.create(line)
    override suspend fun update(line: BudgetLine): Result<Unit> = remote.update(line)
    override suspend fun delete(id: Long): Result<Unit> = remote.delete(id)
}
