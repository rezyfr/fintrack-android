package com.fidriyanto.banktracker.data.datasource.routing

import com.fidriyanto.banktracker.data.datasource.StorageSettingsDataSource
import com.fidriyanto.banktracker.data.datasource.offline.OfflineAppSettingsDataSource
import com.fidriyanto.banktracker.data.datasource.remote.AppSettingsRemoteDataSource
import com.fidriyanto.banktracker.data.datasource.remote.AppSettingsRemoteDataSourceImpl
import com.fidriyanto.banktracker.domain.model.CurrencySettings
import javax.inject.Inject
import javax.inject.Singleton

// ac: offline-mode - sends each call to AppSettingsRemoteDataSourceImpl or OfflineAppSettingsDataSource, by the storage the user chose.
@Singleton
class AppSettingsRouter @Inject constructor(
    storage: StorageSettingsDataSource,
    server: AppSettingsRemoteDataSourceImpl,
    offline: OfflineAppSettingsDataSource,
) : AppSettingsRemoteDataSource {
    private val pick = StorageSwitch<AppSettingsRemoteDataSource>(storage, server, offline)

    override suspend fun get(): CurrencySettings? = pick().get()

    override suspend fun save(settings: CurrencySettings) = pick().save(settings)
}
