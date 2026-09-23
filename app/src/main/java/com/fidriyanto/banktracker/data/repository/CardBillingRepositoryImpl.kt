package com.fidriyanto.banktracker.data.repository

import com.fidriyanto.banktracker.data.datasource.remote.CardBillingRemoteDataSource
import com.fidriyanto.banktracker.domain.model.CardBilling
import javax.inject.Inject

class CardBillingRepositoryImpl @Inject constructor(
    private val remote: CardBillingRemoteDataSource,
) : CardBillingRepository {
    override suspend fun getAll(): Result<List<CardBilling>> = runCatching {
        remote.getCardBilling().map {
            CardBilling(
                wallet = it.wallet,
                cutoffDay = it.cutoffDay,
                dueDay = it.dueDay,
                minPercent = it.minPercent,
                minFullInstallments = it.minFullInstallments,
            )
        }
    }
}
