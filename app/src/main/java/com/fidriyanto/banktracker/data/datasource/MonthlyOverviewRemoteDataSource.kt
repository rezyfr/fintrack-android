package com.fidriyanto.banktracker.data.datasource

import com.fidriyanto.banktracker.data.db.MonthlyOverviewEntity
import com.fidriyanto.banktracker.domain.model.MerchantTotal

interface MonthlyOverviewRemoteDataSource {
    suspend fun fetch(months: List<String>): Result<List<MonthlyOverviewEntity>>
    suspend fun fetchTransportMerchants(months: List<String>): Result<Pair<List<MerchantTotal>, List<MerchantTotal>>>
}
