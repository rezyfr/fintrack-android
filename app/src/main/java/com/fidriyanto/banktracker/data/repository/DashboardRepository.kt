package com.fidriyanto.banktracker.data.repository

import com.fidriyanto.banktracker.data.db.MerchantTotal
import com.fidriyanto.banktracker.data.db.MonthlyOverviewEntity
import kotlinx.coroutines.flow.Flow

interface DashboardRepository {
    fun observeForMonths(months: List<String>): Flow<Pair<List<MonthlyOverviewEntity>, List<MonthlyOverviewEntity>>>
    fun observeTransportBreakdown(fromDate: String, toDate: String): Flow<Pair<List<MerchantTotal>, List<MerchantTotal>>>
    suspend fun refresh(months: List<String>): Result<Unit>
}
