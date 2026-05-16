package com.fidriyanto.banktracker.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MonthlyOverviewDao {
    @Query("SELECT * FROM monthly_overview WHERE month IN (:months) AND currency = :currency")
    fun observeByMonths(months: List<String>, currency: String): Flow<List<MonthlyOverviewEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rows: List<MonthlyOverviewEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBudget(budget: MonthlyBudgetEntity)

    @Query("SELECT * FROM monthly_budget WHERE currency = :currency")
    fun observeBudget(currency: String): Flow<MonthlyBudgetEntity?>
}
