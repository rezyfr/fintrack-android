package com.fidriyanto.banktracker.data.datasource.remote

import android.util.Log
import com.fidriyanto.banktracker.data.datasource.remote.mapper.toDto
import com.fidriyanto.banktracker.data.datasource.remote.mapper.toEntity
import com.fidriyanto.banktracker.data.db.MonthlyBudgetEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetRemoteDataSourceImpl @Inject constructor(
    private val service: SupabaseBudgetService
) : BudgetRemoteDataSource {

    override suspend fun get(): Result<List<MonthlyBudgetEntity>> = runCatching {
        service.getBudgets("*").map { it.toEntity() }
    }.onFailure { Log.e("BudgetRemoteDS", "get failed", it) }

    override suspend fun upsert(budget: MonthlyBudgetEntity): Result<Unit> = runCatching {
        val response = service.upsertBudget("currency", budget.toDto())
        if (!response.isSuccessful) error("Supabase error: HTTP ${response.code()}")
    }.onFailure { Log.e("BudgetRemoteDS", "upsert failed", it) }
}
