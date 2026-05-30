package com.fidriyanto.banktracker.data.repository

import com.fidriyanto.banktracker.data.datasource.remote.BudgetRemoteDataSource
import com.fidriyanto.banktracker.data.db.MonthlyBudgetEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetRepositoryImpl @Inject constructor(
    private val remoteDataSource: BudgetRemoteDataSource
) : BudgetRepository {
    override suspend fun get(): Result<List<MonthlyBudgetEntity>> = remoteDataSource.get()
    override suspend fun upsert(budget: MonthlyBudgetEntity): Result<Unit> = remoteDataSource.upsert(budget)
}
