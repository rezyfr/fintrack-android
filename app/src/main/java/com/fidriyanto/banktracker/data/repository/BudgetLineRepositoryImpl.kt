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
                it.matchWallets, it.matchPattern, it.matchCategories, it.sortOrder)
        }
    }
}
