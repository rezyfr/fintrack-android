package com.fidriyanto.banktracker.data.datasource

import com.fidriyanto.banktracker.data.db.MonthlyBudgetEntity
import com.fidriyanto.banktracker.data.db.MonthlyOverviewDao
import com.fidriyanto.banktracker.data.db.MonthlyOverviewEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MonthlyOverviewLocalDataSourceImpl @Inject constructor(
    private val dao: MonthlyOverviewDao
) : MonthlyOverviewLocalDataSource {
    override fun observeByMonths(months: List<String>, currency: String): Flow<List<MonthlyOverviewEntity>> =
        dao.observeByMonths(months, currency)
    override suspend fun upsertAll(rows: List<MonthlyOverviewEntity>) = dao.upsertAll(rows)
    override suspend fun upsertBudget(budget: MonthlyBudgetEntity) = dao.upsertBudget(budget)
}
