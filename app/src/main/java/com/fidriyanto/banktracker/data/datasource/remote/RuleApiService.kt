package com.fidriyanto.banktracker.data.datasource.remote

import com.fidriyanto.banktracker.data.datasource.remote.dto.RulesDocDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

interface RuleApiService {
    @GET("v1/rules")
    suspend fun getRules(): RulesDocDto

    @PUT("v1/rules")
    suspend fun putRules(@Body doc: RulesDocDto): Response<ResponseBody>
}
