package com.fidriyanto.banktracker.data.datasource.remote

import com.fidriyanto.banktracker.data.datasource.remote.dto.BudgetLineDto
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface BudgetLineApiService {
    @GET("v1/budget-lines")
    suspend fun getBudgetLines(): List<BudgetLineDto>

    // The body is raw JSON, because a plain body drops a null field and a PATCH must be able to clear one.
    @POST("v1/budget-lines")
    suspend fun createBudgetLine(@Body body: RequestBody): Response<ResponseBody>

    @PATCH("v1/budget-lines/{id}")
    suspend fun updateBudgetLine(@Path("id") id: Long, @Body body: RequestBody): Response<ResponseBody>

    @DELETE("v1/budget-lines/{id}")
    suspend fun deleteBudgetLine(@Path("id") id: Long): Response<ResponseBody>
}
