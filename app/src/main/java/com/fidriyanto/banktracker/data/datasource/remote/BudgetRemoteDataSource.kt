package com.fidriyanto.banktracker.data.datasource.remote

import com.fidriyanto.banktracker.data.db.MonthlyBudgetEntity

interface BudgetRemoteDataSource {
    suspend fun get(): Result<List<MonthlyBudgetEntity>>
    suspend fun upsert(budget: MonthlyBudgetEntity): Result<Unit>
}
