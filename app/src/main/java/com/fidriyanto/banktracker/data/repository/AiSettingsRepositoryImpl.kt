package com.fidriyanto.banktracker.data.repository

import com.fidriyanto.banktracker.data.datasource.AiSettingsDataSource
import com.fidriyanto.banktracker.domain.model.AiSettings
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiSettingsRepositoryImpl @Inject constructor(
    private val local: AiSettingsDataSource,
) : AiSettingsRepository {
    override fun observe(): Flow<AiSettings> = local.observe()
    override suspend fun save(settings: AiSettings) = local.save(settings)
}
