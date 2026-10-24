package com.fidriyanto.banktracker.data.datasource.remote

import com.fidriyanto.banktracker.data.datasource.remote.dto.AccountDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Url

// Calls a server the user typed in setup, before the app stores it. Every call takes a full address.
interface ServerProbeApiService {
    @GET
    suspend fun health(@Url url: String): Response<ResponseBody>

    @GET
    suspend fun accounts(@Url url: String, @Header("Authorization") authorization: String): Response<List<AccountDto>>
}
