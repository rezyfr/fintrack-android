package com.fidriyanto.banktracker.data.datasource.remote

import com.fidriyanto.banktracker.data.datasource.remote.dto.InstallmentDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface SupabaseInstallmentService {
    @GET("rest/v1/installments")
    suspend fun getInstallments(
        @Query("select") select: String = "*",
        @Query("order") order: String = "due_day.asc",
    ): List<InstallmentDto>

    // ac: installment-name-edit — PATCH merchant field for a single row
    @PATCH("rest/v1/installments")
    @Headers("Prefer: return=minimal")
    suspend fun updateMerchant(
        @Query("id") idFilter: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>,
    ): Response<ResponseBody>
}
