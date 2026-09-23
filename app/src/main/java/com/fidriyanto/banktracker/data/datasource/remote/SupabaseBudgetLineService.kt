package com.fidriyanto.banktracker.data.datasource.remote

import com.fidriyanto.banktracker.data.datasource.remote.dto.BudgetLineDto
import retrofit2.http.GET
import retrofit2.http.Query

interface SupabaseBudgetLineService {
    @GET("rest/v1/budget_lines")
    suspend fun getBudgetLines(
        @Query("select") select: String = "*",
        @Query("active") active: String = "eq.true",
        @Query("order") order: String = "sort_order.asc",
    ): List<BudgetLineDto>
}
