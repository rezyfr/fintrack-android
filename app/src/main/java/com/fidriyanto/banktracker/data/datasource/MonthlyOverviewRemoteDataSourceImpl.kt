package com.fidriyanto.banktracker.data.datasource

import android.util.Log
import com.fidriyanto.banktracker.data.datasource.remote.SupabaseOverviewService
import com.fidriyanto.banktracker.data.datasource.remote.dto.MonthlyOverviewRequest
import com.fidriyanto.banktracker.data.datasource.remote.dto.TransportMerchantsRequest
import com.fidriyanto.banktracker.data.datasource.remote.mapper.toEntity
import com.fidriyanto.banktracker.data.db.MonthlyOverviewEntity
import com.fidriyanto.banktracker.domain.model.MerchantTotal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MonthlyOverviewRemoteDataSourceImpl @Inject constructor(
    private val service: SupabaseOverviewService
) : MonthlyOverviewRemoteDataSource {

    override suspend fun fetch(months: List<String>): Result<List<MonthlyOverviewEntity>> =
        runCatching {
            val rows = service.getMonthlyOverview(MonthlyOverviewRequest(pMonths = months))
            Log.d("MonthlyOverviewRemoteDS", "fetched ${rows.size} rows for $months")
            rows.map { it.toEntity() }
        }.onFailure { Log.e("MonthlyOverviewRemoteDS", "fetch failed", it) }

    override suspend fun fetchTransportMerchants(months: List<String>): Result<Pair<List<MerchantTotal>, List<MerchantTotal>>> =
        runCatching {
            val rows = service.getTransportMerchants(TransportMerchantsRequest(pMonths = months))
            val thb = rows.filter { it.currency == "THB" }.map { MerchantTotal(it.item, it.amount) }
            val idr = rows.filter { it.currency == "IDR" }.map { MerchantTotal(it.item, it.amount) }
            Pair(thb, idr)
        }.onFailure { Log.e("MonthlyOverviewRemoteDS", "fetchTransportMerchants failed", it) }
}
