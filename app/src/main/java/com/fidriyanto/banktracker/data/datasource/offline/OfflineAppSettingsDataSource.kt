package com.fidriyanto.banktracker.data.datasource.offline

import com.fidriyanto.banktracker.data.datasource.remote.AppSettingsRemoteDataSource
import com.fidriyanto.banktracker.domain.model.CurrencySettings
import javax.inject.Inject
import javax.inject.Singleton

// ac: offline-mode - there is no shared settings row offline. The phone's own copy is the only one, so a
// read finds nothing to apply and a save has nowhere else to go.
@Singleton
class OfflineAppSettingsDataSource @Inject constructor() : AppSettingsRemoteDataSource {
    override suspend fun get(): CurrencySettings? = null

    override suspend fun save(settings: CurrencySettings) = Unit
}
