package com.fidriyanto.banktracker.data.repository

import com.fidriyanto.banktracker.data.db.MonthlyOverviewDao
import com.fidriyanto.banktracker.data.db.MonthlyOverviewEntity
import com.fidriyanto.banktracker.sheets.MonthlyOverviewFetcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DashboardRepository @Inject constructor(
    private val dao: MonthlyOverviewDao,
    private val fetcher: MonthlyOverviewFetcher
) {
    fun observeForMonths(
        months: List<String>
    ): Flow<Pair<List<MonthlyOverviewEntity>, List<MonthlyOverviewEntity>>> =
        combine(
            dao.observeByMonths(months, "THB"),
            dao.observeByMonths(months, "IDR")
        ) { thb, idr -> Pair(thb, idr) }

    suspend fun refresh(): Result<Unit> {
        return try {
            val result = fetcher.fetch()
            result.onSuccess { data ->
                dao.upsertAll(data.rows)
                data.budgets.forEach { dao.upsertBudget(it) }
            }
            result.map { }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
