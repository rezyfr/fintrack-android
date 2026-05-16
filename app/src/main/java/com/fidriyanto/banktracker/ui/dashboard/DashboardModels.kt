package com.fidriyanto.banktracker.ui.dashboard

enum class Period { THIS_MONTH, LAST_MONTH, LAST_3_MONTHS }

sealed class DashboardUiState {
    object NotSignedIn : DashboardUiState()
    object LoadingNoCache : DashboardUiState()
    data class Loaded(
        val period: Period,
        val thb: CurrencySummary,
        val idr: CurrencySummary,
        val isRefreshing: Boolean,
        val lastUpdated: String?,
        val refreshError: Boolean
    ) : DashboardUiState()
}

data class CurrencySummary(
    val totalIncome: Double,
    val totalExpenses: Double,
    val net: Double,
    val categoryBreakdown: List<CategoryRow>
)

data class CategoryRow(
    val category: String,
    val amount: Double,
    val percentage: Float
)
