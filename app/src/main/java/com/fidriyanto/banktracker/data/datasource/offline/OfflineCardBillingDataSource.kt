package com.fidriyanto.banktracker.data.datasource.offline

import com.fidriyanto.banktracker.data.datasource.remote.CardBillingRemoteDataSource
import com.fidriyanto.banktracker.data.datasource.remote.dto.CardBillingDto
import com.fidriyanto.banktracker.data.db.LedgerDao
import javax.inject.Inject
import javax.inject.Singleton

// ac: offline-mode - the card billing setups in the offline store. The app has no editor for them yet,
// so the list stays empty until a backup brings some in.
@Singleton
class OfflineCardBillingDataSource @Inject constructor(
    private val dao: LedgerDao,
) : CardBillingRemoteDataSource {
    override suspend fun getCardBilling(): List<CardBillingDto> = dao.cardBilling().map { it.toDto() }
}
