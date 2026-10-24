package com.fidriyanto.banktracker.data.datasource.remote

import com.fidriyanto.banktracker.data.datasource.remote.dto.CardBillingDto
import retrofit2.http.GET

interface CardBillingApiService {
    @GET("v1/card-billing")
    suspend fun getCardBilling(): List<CardBillingDto>
}
