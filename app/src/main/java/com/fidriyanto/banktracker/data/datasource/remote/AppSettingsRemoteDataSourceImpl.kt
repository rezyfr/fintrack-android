package com.fidriyanto.banktracker.data.datasource.remote

import com.fidriyanto.banktracker.data.datasource.remote.dto.AppSettingsDto
import com.fidriyanto.banktracker.data.datasource.remote.dto.CurrencyDto
import com.fidriyanto.banktracker.domain.model.CurrencyOverride
import com.fidriyanto.banktracker.domain.model.CurrencySettings
import javax.inject.Inject

class AppSettingsRemoteDataSourceImpl @Inject constructor(
    private val service: AppSettingsApiService,
) : AppSettingsRemoteDataSource {
    override suspend fun get(): CurrencySettings? {
        val response = service.getSettings()
        if (response.code() == 404) return null
        if (!response.isSuccessful) error("API error: HTTP ${response.code()}")
        val row = response.body() ?: return null
        val overrides = row.currencies.map { CurrencyOverride(it.code, it.label.ifBlank { it.code }, it.rate?.takeIf { r -> r > 0 }) }
        return CurrencySettings(row.homeCode, row.homeLabel, row.cycleStartDay, overrides)
    }

    override suspend fun save(settings: CurrencySettings) {
        val dto = AppSettingsDto(
            homeCode = settings.homeCode,
            homeLabel = settings.homeLabel,
            cycleStartDay = settings.cycleStartDay,
            currencies = settings.overrides.map { CurrencyDto(it.code, it.label, it.rate) },
        )
        val response = service.putSettings(dto)
        if (!response.isSuccessful) error("API error: HTTP ${response.code()}")
    }
}
