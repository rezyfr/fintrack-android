package com.fidriyanto.banktracker.data.repository

import com.fidriyanto.banktracker.domain.model.CardBilling

interface CardBillingRepository {
    suspend fun getAll(): Result<List<CardBilling>>
}
