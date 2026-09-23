package com.fidriyanto.banktracker.data.datasource.remote

import com.fidriyanto.banktracker.data.datasource.remote.dto.CardBillingDto
import retrofit2.http.GET
import retrofit2.http.Query

interface SupabaseCardBillingService {
    @GET("rest/v1/card_billing")
    suspend fun getCardBilling(@Query("select") select: String = "*"): List<CardBillingDto>
}
