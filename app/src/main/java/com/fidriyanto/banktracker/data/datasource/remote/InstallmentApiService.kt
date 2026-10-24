package com.fidriyanto.banktracker.data.datasource.remote

import com.fidriyanto.banktracker.data.datasource.remote.dto.InstallmentCreateDto
import com.fidriyanto.banktracker.data.datasource.remote.dto.InstallmentDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface InstallmentApiService {
    @GET("v1/installments")
    suspend fun getInstallments(): List<InstallmentDto>

    // ac: installment-name-edit — a PATCH changes only the fields in the body
    @PATCH("v1/installments/{id}")
    suspend fun updateInstallment(
        @Path("id") id: Long,
        @Body body: Map<String, @JvmSuppressWildcards Any>,
    ): Response<ResponseBody>

    // A new plan. The server answers 201 with the plan.
    @POST("v1/installments")
    suspend fun createInstallment(@Body body: InstallmentCreateDto): Response<InstallmentDto>

    // The server answers 204. A transaction that pointed to the plan keeps its row and loses the link.
    @DELETE("v1/installments/{id}")
    suspend fun deleteInstallment(@Path("id") id: Long): Response<ResponseBody>
}
