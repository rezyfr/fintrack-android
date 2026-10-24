package com.fidriyanto.banktracker.domain.model

// The day a pay cycle starts on an installation that never chose one.
const val LEGACY_CYCLE_START_DAY = 26

// A label (the book name) and a starting exchange rate for one non-home currency. rate is home units per
// one unit of this currency, used until transfers give a rate. Null means unset.
data class CurrencyOverride(val code: String, val label: String = code, val rate: Double? = null)

// The home currency and the choices the user made. The currencies in use are not stored here: they are
// the home currency plus the currency of every account. The defaults are what this app shipped with:
// IDR as home, and THB labeled "Thailand". They apply to an installation that never saved settings.
data class CurrencySettings(
    val homeCode: String = "IDR",
    val homeLabel: String = "Indonesia",
    val cycleStartDay: Int = LEGACY_CYCLE_START_DAY,
    val overrides: List<CurrencyOverride> = listOf(CurrencyOverride("THB", "Thailand")),
) {
    // The currency shown for a transaction with no wallet, which only very old rows have. Those were in the
    // foreign currency, so it is the first other currency, or the home currency when there is none.
    val noWalletCurrency: String get() = overrides.firstOrNull()?.code ?: homeCode

    fun overrideFor(code: String): CurrencyOverride? = overrides.firstOrNull { it.code == code }

    // The book name of a currency: the label the user chose, or the code.
    fun labelFor(code: String): String =
        if (code == homeCode) homeLabel else overrideFor(code)?.label?.takeIf { it.isNotBlank() } ?: code
}

// The home currency first, then the currency of each account in account order, without repeats.
fun currenciesInUse(homeCode: String, accounts: List<Account>): List<String> =
    (listOf(homeCode) + accounts.map { it.currency }).filter { it.isNotBlank() }.distinct()

// Common currency symbols for display. Falls back to the code itself (for example "USD 10")
// when a currency isn't in this table, rather than guessing a symbol.
internal val CURRENCY_SYMBOLS = mapOf(
    "IDR" to "Rp", "THB" to "฿", "USD" to "$", "EUR" to "€", "GBP" to "£",
    "SGD" to "S$", "JPY" to "¥", "MYR" to "RM", "AUD" to "A$", "VND" to "₫",
    "PHP" to "₱", "CNY" to "¥",
)

fun currencySymbol(code: String): String = CURRENCY_SYMBOLS[code] ?: "$code "

// Currencies conventionally shown with no decimal places.
private val ZERO_DECIMAL_CURRENCIES = setOf("IDR", "JPY", "VND", "KRW")

fun isZeroDecimalCurrency(code: String): Boolean = code in ZERO_DECIMAL_CURRENCIES
