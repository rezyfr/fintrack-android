package com.fidriyanto.banktracker.ui.dashboard

import com.fidriyanto.banktracker.domain.model.CurrencySummary

sealed class DashboardUiState {
    object NotSignedIn : DashboardUiState()
    object LoadingNoCache : DashboardUiState()
    data class Loaded(
        val cycleOffset: Int,
        val byCurrency: Map<String, CurrencySummary>,
        val combined: CurrencySummary,
        val fromIso: String,
        val toIso: String,
        val isRefreshing: Boolean,
    ) : DashboardUiState()
}
