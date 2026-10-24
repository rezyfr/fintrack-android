package com.fidriyanto.banktracker.data.repository

import com.fidriyanto.banktracker.domain.model.CurrencySettings
import kotlinx.coroutines.flow.Flow

interface CurrencySettingsRepository {
    fun observe(): Flow<CurrencySettings>
    suspend fun save(settings: CurrencySettings)
    // Reads the shared settings row and keeps it on this phone. With no row the phone keeps its own.
    suspend fun refresh(): Result<Unit>
}
