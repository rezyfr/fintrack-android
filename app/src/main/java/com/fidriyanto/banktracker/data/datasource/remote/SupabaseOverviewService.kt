package com.fidriyanto.banktracker.data.datasource.remote

import com.fidriyanto.banktracker.data.datasource.remote.dto.MonthlyOverviewRequest
import com.fidriyanto.banktracker.data.datasource.remote.dto.MonthlyOverviewRowDto
import retrofit2.http.Body
import retrofit2.http.POST

interface SupabaseOverviewService {
    @POST("rest/v1/rpc/get_monthly_overview")
    suspend fun getMonthlyOverview(@Body body: MonthlyOverviewRequest): List<MonthlyOverviewRowDto>
}
