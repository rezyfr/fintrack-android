package com.fidriyanto.banktracker.data.datasource

import com.fidriyanto.banktracker.domain.model.CurrencySettings
import kotlinx.coroutines.flow.Flow

interface CurrencySettingsDataSource {
    fun observe(): Flow<CurrencySettings>
    suspend fun save(settings: CurrencySettings)
}
