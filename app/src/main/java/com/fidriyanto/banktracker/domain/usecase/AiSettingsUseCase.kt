package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.repository.AiSettingsRepository
import com.fidriyanto.banktracker.domain.model.AiSettings
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class AiSettingsUseCase @Inject constructor(
    private val repository: AiSettingsRepository,
) {
    fun observe(): Flow<AiSettings> = repository.observe()
    suspend fun save(settings: AiSettings) = repository.save(settings)
}
