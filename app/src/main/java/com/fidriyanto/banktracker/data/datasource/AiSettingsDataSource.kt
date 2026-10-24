package com.fidriyanto.banktracker.data.datasource

import com.fidriyanto.banktracker.domain.model.AiSettings
import kotlinx.coroutines.flow.Flow

interface AiSettingsDataSource {
    fun observe(): Flow<AiSettings>
    suspend fun save(settings: AiSettings)
}
