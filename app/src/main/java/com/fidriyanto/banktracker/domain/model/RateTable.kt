package com.fidriyanto.banktracker.domain.model

// Home units per one unit of each currency. The home currency is 1. A currency with no rate counts 1, so
// an amount is never lost.
class RateTable(val homeCode: String, private val rates: Map<String, Double> = emptyMap()) {
    fun homePer(code: String): Double = if (code == homeCode) 1.0 else rates[code]?.takeIf { it > 0 } ?: 1.0

    fun toHome(code: String, amount: Double): Double = amount * homePer(code)

    fun convert(amount: Double, from: String, to: String): Double =
        if (from == to) amount else amount * homePer(from) / homePer(to)
}
