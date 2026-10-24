package com.fidriyanto.banktracker.data.datasource.remote

import com.fidriyanto.banktracker.data.datasource.remote.dto.AppSettingsDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

interface AppSettingsApiService {
    // The server answers 404 until the first-run setup saves the settings.
    @GET("v1/settings")
    suspend fun getSettings(): Response<AppSettingsDto>

    @PUT("v1/settings")
    suspend fun putSettings(@Body settings: AppSettingsDto): Response<ResponseBody>
}
