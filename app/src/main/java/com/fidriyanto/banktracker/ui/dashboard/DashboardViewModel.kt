package com.fidriyanto.banktracker.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fidriyanto.banktracker.auth.GoogleAuthManager
import com.fidriyanto.banktracker.data.db.MonthlyOverviewEntity
import com.fidriyanto.banktracker.data.repository.DashboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: DashboardRepository,
    private val authManager: GoogleAuthManager
) : ViewModel() {

    private val _period = MutableStateFlow(Period.THIS_MONTH)
    val period = _period.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    private val _refreshError = MutableStateFlow(false)
    @Volatile private var lastFetchedAt: Long? = null

    val state: StateFlow<DashboardUiState> = _period
        .flatMapLatest { p ->
            if (!authManager.isSignedIn()) return@flatMapLatest flowOf(DashboardUiState.NotSignedIn)
            val months = monthsFor(p)
            combine(
                repository.observeForMonths(months),
                _isRefreshing,
                _refreshError
            ) { rows, refreshing, error ->
                val thbRows = rows.first
                val idrRows = rows.second
                if (thbRows.isEmpty() && idrRows.isEmpty() && !refreshing && !error) {
                    DashboardUiState.LoadingNoCache
                } else {
                    DashboardUiState.Loaded(
                        period = p,
                        thb = buildSummary(thbRows),
                        idr = buildSummary(idrRows),
                        isRefreshing = refreshing,
                        lastUpdated = lastFetchedAt?.let { formatRelativeTime(it) },
                        refreshError = error
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState.LoadingNoCache)

    init {
        if (authManager.isSignedIn()) refresh()
    }

    fun selectPeriod(p: Period) {
        _period.value = p
        _refreshError.value = false
        refresh()
    }

    fun refresh() = viewModelScope.launch {
        _isRefreshing.value = true
        _refreshError.value = false
        val result = repository.refresh()
        if (result.isSuccess) lastFetchedAt = System.currentTimeMillis()
        _refreshError.value = result.isFailure
        _isRefreshing.value = false
    }

    private fun monthsFor(period: Period): List<String> {
        val now = LocalDate.now()
        return when (period) {
            Period.THIS_MONTH -> listOf(monthLabel(now))
            Period.LAST_MONTH -> listOf(monthLabel(now.minusMonths(1)))
            Period.LAST_3_MONTHS -> (1L..3L).map { monthLabel(now.minusMonths(it)) }
        }
    }

    private fun monthLabel(date: LocalDate): String {
        val name = date.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
        return "$name ${date.year}"
    }

    private fun formatRelativeTime(ms: Long): String {
        val mins = (System.currentTimeMillis() - ms) / 60_000
        return when {
            mins < 1L -> "just now"
            mins == 1L -> "1 min ago"
            mins < 60L -> "$mins mins ago"
            else -> "${mins / 60}h ago"
        }
    }

    private fun buildSummary(rows: List<MonthlyOverviewEntity>): CurrencySummary {
        val totalIncome = rows.sumOf { it.income }
        val totalExpenses = rows.sumOf { it.totalExpenditure }
        val net = totalIncome - totalExpenses
        val breakdown = listOf(
            "Bills" to rows.sumOf { it.bills },
            "Subscriptions" to rows.sumOf { it.subscriptions },
            "Entertainment" to rows.sumOf { it.entertainment },
            "Food & Drink" to rows.sumOf { it.foodDrink },
            "Groceries" to rows.sumOf { it.groceries },
            "Health & Wellbeing" to rows.sumOf { it.healthWellbeing },
            "Other" to rows.sumOf { it.other },
            "Shopping" to rows.sumOf { it.shopping },
            "Transport" to rows.sumOf { it.transport },
            "Travel" to rows.sumOf { it.travel },
            "Business" to rows.sumOf { it.business },
            "Gifts" to rows.sumOf { it.gifts }
        )
            .filter { it.second > 0.0 }
            .sortedByDescending { it.second }
            .map { (cat, amt) ->
                CategoryRow(
                    category = cat,
                    amount = amt,
                    percentage = if (totalExpenses > 0.0) (amt / totalExpenses).toFloat() else 0f
                )
            }
        return CurrencySummary(totalIncome, totalExpenses, net, breakdown)
    }
}
