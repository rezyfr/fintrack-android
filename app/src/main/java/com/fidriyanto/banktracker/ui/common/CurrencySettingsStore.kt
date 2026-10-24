package com.fidriyanto.banktracker.ui.common

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.fidriyanto.banktracker.domain.model.CurrencySettings
import com.fidriyanto.banktracker.domain.usecase.PayCycle

// ac: configurable-currency-pair - the home currency and the labels and rates the user chose, read by plain
// Composables and ViewModels alike. Falls back to CurrencySettings()'s IDR/Indonesia and
// THB/Thailand defaults until any screen that depends on GetCurrencySettingsUseCase has loaded at least once.
object CurrencySettingsStore {
    var current by mutableStateOf(CurrencySettings())
        private set

    fun hydrate(settings: CurrencySettings) {
        current = settings
        PayCycle.startDay = settings.cycleStartDay
    }
}
