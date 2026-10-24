package com.fidriyanto.banktracker.domain.model

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

// ac: multicurrency-money-formatting — the one place that turns an amount and a currency code into
// display text. Every screen calls this with the currency of the amount being shown, so there is no
// per-currency helper to pick by mistake. Zero-decimal currencies show no decimals. Other currencies
// show up to 2 decimals and drop them for whole amounts. A negative amount gets a leading "-", and
// showSign adds a leading "+" to a non-negative amount.
fun formatMoney(currency: String, amount: Double, showSign: Boolean = false): String {
    val fmt = NumberFormat.getNumberInstance(Locale.US).apply {
        val digits = if (isZeroDecimalCurrency(currency) || abs(amount) % 1.0 == 0.0) 0 else 2
        minimumFractionDigits = digits
        maximumFractionDigits = digits
    }
    val body = fmt.format(abs(amount))
    val symbol = currencySymbol(currency).trim()
    val separator = if (symbol.last().isLetter()) " " else ""
    val isZero = body.none { it in '1'..'9' }
    val sign = when {
        amount < 0 && !isZero -> "-"
        showSign && amount >= 0 || showSign && isZero -> "+"
        else -> ""
    }
    return "$sign$symbol$separator$body"
}

// ac: insights-compact-breakdown — a short amount for tight rows: up to 999 as formatMoney, then
// thousands as "k" and millions as "M" with at most one decimal. Example: Rp 14.6M, ฿1.1k.
fun formatMoneyCompact(currency: String, amount: Double): String {
    val v = abs(amount)
    if (v < 1_000) return formatMoney(currency, amount)
    val (scaled, unit) = if (v < 1_000_000) v / 1_000 to "k" else v / 1_000_000 to "M"
    val body = if (scaled < 100) String.format(Locale.US, "%.1f", scaled).removeSuffix(".0") else String.format(Locale.US, "%.0f", scaled)
    val symbol = currencySymbol(currency).trim()
    val separator = if (symbol.last().isLetter()) " " else ""
    return (if (amount < 0) "-" else "") + symbol + separator + body + unit
}
