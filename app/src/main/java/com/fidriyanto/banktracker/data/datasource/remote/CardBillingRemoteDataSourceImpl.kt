package com.fidriyanto.banktracker.data.datasource.remote

import com.fidriyanto.banktracker.data.datasource.remote.dto.CardBillingDto
import javax.inject.Inject

class CardBillingRemoteDataSourceImpl @Inject constructor(
    private val service: CardBillingApiService,
) : CardBillingRemoteDataSource {
    override suspend fun getCardBilling(): List<CardBillingDto> = service.getCardBilling()
}
