package com.fidriyanto.banktracker.ui.dashboard

import com.fidriyanto.banktracker.domain.model.CurrencySummary
import com.fidriyanto.banktracker.domain.model.Period

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
