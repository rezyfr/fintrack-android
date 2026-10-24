package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.repository.CurrencySettingsRepository
import com.fidriyanto.banktracker.domain.model.CurrencySettings
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCurrencySettingsUseCase @Inject constructor(
    private val currencySettingsRepository: CurrencySettingsRepository,
) {
    // ac: configurable-currency-pair — every screen that shows or buckets by currency reads the
    // configured pair instead of a compiled-in "THB"/"IDR" literal
    operator fun invoke(): Flow<CurrencySettings> = currencySettingsRepository.observe()

    // Pulls the shared settings row. The web app and a second phone write the same row.
    suspend fun refresh(): Result<Unit> = currencySettingsRepository.refresh()
}
