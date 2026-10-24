package com.fidriyanto.banktracker.domain.model

// ac: exchange-rate-from-transfers - the rate used when there is no transfer to derive one from. The rate
// the user typed for that currency wins. Otherwise 535 rupiah per baht, which this app shipped with, and
// 1 to 1 for any other currency. The rate is home units per one unit of [code].
fun fallbackRate(settings: CurrencySettings, code: String): Double =
    settings.overrideFor(code)?.rate?.takeIf { it > 0 }
        ?: if (settings.homeCode == "IDR" && code == "THB") 535.0 else 1.0

// The median of [rates], or null when there are none. Shared so Android and web pick the same value.
fun medianRate(rates: List<Double>): Double? = rates.sorted().let { if (it.isEmpty()) null else it[it.size / 2] }
