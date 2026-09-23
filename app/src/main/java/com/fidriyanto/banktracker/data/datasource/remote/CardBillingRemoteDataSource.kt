package com.fidriyanto.banktracker.data.datasource.remote

import com.fidriyanto.banktracker.data.datasource.remote.dto.CardBillingDto

interface CardBillingRemoteDataSource {
    suspend fun getCardBilling(): List<CardBillingDto>
}
