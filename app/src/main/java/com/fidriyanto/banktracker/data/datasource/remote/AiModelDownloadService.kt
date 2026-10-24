package com.fidriyanto.banktracker.data.datasource.remote

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Streaming
import retrofit2.http.Url

interface AiModelDownloadService {
    @Streaming
    @GET
    suspend fun fetch(@Url url: String, @Header("Range") range: String?): Response<ResponseBody>
}
