package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.repository.TransactionRepository
import com.fidriyanto.banktracker.domain.model.CurrencySettings
import com.fidriyanto.banktracker.domain.model.RateTable
import com.fidriyanto.banktracker.domain.model.fallbackRate
import com.fidriyanto.banktracker.domain.model.medianRate

// ac: exchange-rate-from-transfers - the rate of every non-home currency in the user's accounts. Each rate is
// home units per one unit, taken from the user's own transfers: the median of the last 10 transfers from an
// account in that currency into an account in the home currency. This is the rule web Budget uses
// (getRecentFxRate). A currency with no such transfer falls back to its typed rate, then to the default.
internal suspend fun TransactionRepository.rateTable(
    currencyByWallet: Map<String, String>,
    settings: CurrencySettings,
): RateTable {
    val to = currencyByWallet.filterValues { it == settings.homeCode }.keys.toList()
    val rates = currencyByWallet.values.toSet().filter { it != settings.homeCode }.associateWith { code ->
        val from = currencyByWallet.filterValues { it == code }.keys.toList()
        medianRate(recentTransferRates(from, to).getOrElse { emptyList() }) ?: fallbackRate(settings, code)
    }
    return RateTable(settings.homeCode, rates)
}
