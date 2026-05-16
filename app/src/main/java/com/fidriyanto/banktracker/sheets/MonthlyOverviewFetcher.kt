package com.fidriyanto.banktracker.sheets

import com.fidriyanto.banktracker.data.db.MonthlyBudgetEntity
import com.fidriyanto.banktracker.data.db.MonthlyOverviewEntity

interface MonthlyOverviewFetcher {
    data class FetchResult(
        val rows: List<MonthlyOverviewEntity>,
        val budgets: List<MonthlyBudgetEntity>
    )

    suspend fun fetch(): Result<FetchResult>
}
