package com.fidriyanto.banktracker.data.datasource.routing

import com.fidriyanto.banktracker.data.datasource.StorageSettingsDataSource
import com.fidriyanto.banktracker.data.datasource.offline.OfflineCardBillingDataSource
import com.fidriyanto.banktracker.data.datasource.remote.CardBillingRemoteDataSource
import com.fidriyanto.banktracker.data.datasource.remote.CardBillingRemoteDataSourceImpl
import com.fidriyanto.banktracker.data.datasource.remote.dto.CardBillingDto
import javax.inject.Inject
import javax.inject.Singleton

// ac: offline-mode - sends each call to CardBillingRemoteDataSourceImpl or OfflineCardBillingDataSource, by the storage the user chose.
@Singleton
class CardBillingRouter @Inject constructor(
    storage: StorageSettingsDataSource,
    server: CardBillingRemoteDataSourceImpl,
    offline: OfflineCardBillingDataSource,
) : CardBillingRemoteDataSource {
    private val pick = StorageSwitch<CardBillingRemoteDataSource>(storage, server, offline)

    override suspend fun getCardBilling(): List<CardBillingDto> = pick().getCardBilling()
}
