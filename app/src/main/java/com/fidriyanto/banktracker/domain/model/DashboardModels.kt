package com.fidriyanto.banktracker.domain.model

// previousAmount is the category's spend in the previous period of the same length. It is null
// when no previous period was loaded.
data class CategoryRow(
    val category: String,
    val amount: Double,
    val percentage: Float,
    val previousAmount: Double? = null,
)

// One wallet's share of the period's expense total.
data class WalletRow(val wallet: String, val amount: Double, val percentage: Float)

// Money in and money out of one pay cycle. endIso is the cycle's last day, so a cycle that ends on
// 25 Oct is labelled October.
data class MonthTrendPoint(val endIso: String, val moneyIn: Double, val moneyOut: Double, val inProgress: Boolean)

// moneyIn and moneyOut count transfers that cross currency zones, so net is the real change in money.
// categorySpend is the expense total that the category breakdown splits.
data class CurrencySummary(
    val moneyIn: Double,
    val moneyOut: Double,
    val net: Double,
    val categoryBreakdown: List<CategoryRow>,
    // ac: insights-subcategory-breakdown — per-category subcategory rows (None for unset)
    val subcategoryBreakdown: Map<String, List<CategoryRow>> = emptyMap(),
    val categorySpend: Double = 0.0,
    // ac: insights-paid-with — expense total per wallet, largest first
    val walletBreakdown: List<WalletRow> = emptyList(),
    // ac: insights-largest-expenses — the period's largest expense transactions, largest first
    val topExpenses: List<TransactionUiModel> = emptyList(),
    // ac: insights-daily-heatmap — expense total per ISO date
    val dailySpend: Map<String, Double> = emptyMap(),
    // ac: insights-six-month-trend — the last six pay cycles, oldest first
    val monthlyTrend: List<MonthTrendPoint> = emptyList(),
) {
    fun isEmpty() = moneyIn == 0.0 && moneyOut == 0.0 && categorySpend == 0.0
}

// fromIso and toIso are the selected pay cycle. byCurrency holds one summary for each currency in use.
// combined holds all of them converted to the home currency at the rate from the user's own transfers
// (exchange-rate-from-transfers).
data class DashboardSummaries(
    val byCurrency: Map<String, CurrencySummary>,
    val combined: CurrencySummary,
    val fromIso: String = "",
    val toIso: String = "",
) {
    fun isEmpty() = byCurrency.values.all { it.isEmpty() }
}
