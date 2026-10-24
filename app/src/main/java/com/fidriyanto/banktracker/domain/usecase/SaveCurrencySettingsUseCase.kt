package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.repository.CurrencySettingsRepository
import com.fidriyanto.banktracker.domain.model.CurrencyOverride
import com.fidriyanto.banktracker.domain.model.CurrencySettings
import javax.inject.Inject

class SaveCurrencySettingsUseCase @Inject constructor(
    private val currencySettingsRepository: CurrencySettingsRepository,
) {
    // ac: configurable-currency-pair - the user can rename the home currency, name the book of each other
    // currency, and give each a starting rate, from Settings
    suspend operator fun invoke(settings: CurrencySettings) {
        val home = settings.homeCode.trim().uppercase()
        val overrides = settings.overrides
            .map { CurrencyOverride(it.code.trim().uppercase(), it.label.trim().ifEmpty { it.code.trim().uppercase() }, it.rate?.takeIf { r -> r > 0 }) }
            .filter { it.code.isNotEmpty() && it.code != home }
            .distinctBy { it.code }
        currencySettingsRepository.save(
            settings.copy(homeCode = home, homeLabel = settings.homeLabel.trim(), cycleStartDay = settings.cycleStartDay.coerceIn(1, 28), overrides = overrides),
        )
    }
}
