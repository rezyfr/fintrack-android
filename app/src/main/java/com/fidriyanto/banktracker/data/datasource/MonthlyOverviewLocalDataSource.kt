package com.fidriyanto.banktracker.data.datasource

import com.fidriyanto.banktracker.data.db.MonthlyBudgetEntity
import com.fidriyanto.banktracker.data.db.MonthlyOverviewEntity
import kotlinx.coroutines.flow.Flow

interface MonthlyOverviewLocalDataSource {
    fun observeByMonths(months: List<String>, currency: String): Flow<List<MonthlyOverviewEntity>>
    suspend fun upsertAll(rows: List<MonthlyOverviewEntity>)
    suspend fun upsertBudget(budget: MonthlyBudgetEntity)
}
