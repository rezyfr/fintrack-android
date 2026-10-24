package com.fidriyanto.banktracker.data.datasource.remote

import com.fidriyanto.banktracker.domain.model.CurrencySettings

interface AppSettingsRemoteDataSource {
    // The shared settings row, or null when the user never saved settings.
    suspend fun get(): CurrencySettings?
    suspend fun save(settings: CurrencySettings)
}
