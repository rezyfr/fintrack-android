package com.fidriyanto.banktracker.data.datasource.remote

import com.fidriyanto.banktracker.data.datasource.remote.dto.BudgetDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

interface SupabaseBudgetService {
    @GET("rest/v1/budgets")
    suspend fun getBudgets(@Query("select") select: String): List<BudgetDto>

    @POST("rest/v1/budgets")
    @Headers("Prefer: resolution=merge-duplicates,return=minimal")
    suspend fun upsertBudget(
        @Query("on_conflict") onConflict: String,
        @Body budget: BudgetDto
    ): Response<ResponseBody>
}
