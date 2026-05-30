package com.fidriyanto.banktracker.data.datasource

import com.fidriyanto.banktracker.data.db.MonthlyOverviewEntity

interface MonthlyOverviewRemoteDataSource {
    suspend fun fetch(months: List<String>): Result<List<MonthlyOverviewEntity>>
}
