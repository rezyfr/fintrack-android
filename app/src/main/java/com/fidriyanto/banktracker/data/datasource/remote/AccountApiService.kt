package com.fidriyanto.banktracker.data.datasource.remote

import com.fidriyanto.banktracker.data.datasource.remote.dto.AccountDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

interface AccountApiService {
    @GET("v1/accounts")
    suspend fun getAccounts(): List<AccountDto>

    @PUT("v1/accounts/{id}")
    suspend fun putAccount(
        @Path("id") id: String,
        @Body account: AccountDto,
    ): Response<ResponseBody>

    @DELETE("v1/accounts/{id}")
    suspend fun deleteAccount(@Path("id") id: String): Response<ResponseBody>
}
