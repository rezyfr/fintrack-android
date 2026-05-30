package com.fidriyanto.banktracker.domain.model

enum class Period { THIS_MONTH, LAST_MONTH, LAST_3_MONTHS }

data class CategoryRow(val category: String, val amount: Double, val percentage: Float)

data class MerchantRow(val merchant: String, val amount: Double, val percentage: Float)

data class MerchantTotal(val merchant: String, val amount: Double)

data class MonthlyOverviewSummary(
    val income: Double,
    val totalExpenditure: Double,
    val bills: Double,
    val subscriptions: Double,
    val entertainment: Double,
    val foodDrink: Double,
    val groceries: Double,
    val healthWellbeing: Double,
    val family: Double,
    val other: Double,
    val shopping: Double,
    val transport: Double,
    val travel: Double,
    val business: Double,
    val gifts: Double
)

data class CurrencySummary(
    val totalIncome: Double,
    val totalExpenses: Double,
    val net: Double,
    val categoryBreakdown: List<CategoryRow>,
    val transportBreakdown: List<MerchantRow> = emptyList()
) {
    fun isEmpty() = totalIncome == 0.0 && totalExpenses == 0.0
}

data class DashboardSummaryPair(val thb: CurrencySummary, val idr: CurrencySummary) {
    fun isEmpty() = thb.isEmpty() && idr.isEmpty()
}
