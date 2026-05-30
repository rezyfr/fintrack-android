package com.fidriyanto.banktracker.data.datasource

import android.util.Log
import com.fidriyanto.banktracker.data.datasource.remote.SupabaseOverviewService
import com.fidriyanto.banktracker.data.datasource.remote.dto.MonthlyOverviewRequest
import com.fidriyanto.banktracker.data.datasource.remote.mapper.toEntity
import com.fidriyanto.banktracker.data.db.MonthlyOverviewEntity
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
}
