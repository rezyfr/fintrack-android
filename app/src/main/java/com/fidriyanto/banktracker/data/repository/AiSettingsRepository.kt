package com.fidriyanto.banktracker.data.repository

import com.fidriyanto.banktracker.domain.model.AiSettings
import kotlinx.coroutines.flow.Flow

interface AiSettingsRepository {
    fun observe(): Flow<AiSettings>
    suspend fun save(settings: AiSettings)
}
