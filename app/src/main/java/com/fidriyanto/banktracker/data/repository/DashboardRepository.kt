package com.fidriyanto.banktracker.data.repository

import com.fidriyanto.banktracker.domain.model.MerchantTotal
import com.fidriyanto.banktracker.domain.model.MonthlyOverviewSummary
import kotlinx.coroutines.flow.Flow

interface DashboardRepository {
    fun observeForMonths(months: List<String>): Flow<Pair<List<MonthlyOverviewSummary>, List<MonthlyOverviewSummary>>>
    // ac: transport-provider-breakdown — THB and IDR transport breakdowns are shown independently
    fun observeTransportMerchants(): Flow<Pair<List<MerchantTotal>, List<MerchantTotal>>>
    suspend fun refresh(months: List<String>): Result<Unit>
}
