package com.fidriyanto.banktracker.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fidriyanto.banktracker.domain.model.Period
import com.fidriyanto.banktracker.domain.usecase.GetDashboardDataUseCase
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
    private val useCase: GetDashboardDataUseCase
) : ViewModel() {

    private val _period       = MutableStateFlow(Period.THIS_MONTH)
    private val _customFrom   = MutableStateFlow<String?>(null)
    private val _customTo     = MutableStateFlow<String?>(null)
    private val _isRefreshing = MutableStateFlow(false)
    private val _refreshError = MutableStateFlow(false)
    @Volatile private var lastFetchedAt: Long? = null

    val period     = _period.asStateFlow()
    val customFrom = _customFrom.asStateFlow()
    val customTo   = _customTo.asStateFlow()
    val isCustom   = combine(_customFrom, _customTo) { f, t -> f != null && t != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val availableMonths: List<String> = (0..23).map { i ->
        val d = LocalDate.now().minusMonths(i.toLong())
        "${d.year}-${d.monthValue.toString().padStart(2, '0')}"
    }

    val state: StateFlow<DashboardUiState> =
        combine(_period, _customFrom, _customTo) { p, from, to -> Triple(p, from, to) }
            .flatMapLatest { (p, from, to) ->
                combine(
                    useCase.observe(p, from, to),
                    _isRefreshing,
                    _refreshError
                ) { summaryPair, refreshing, error ->
                    if (summaryPair.isEmpty() && !refreshing && !error)
                        DashboardUiState.LoadingNoCache
                    else
                        DashboardUiState.Loaded(
                            period       = p,
                            thb          = summaryPair.thb,
                            idr          = summaryPair.idr,
                            isRefreshing = refreshing,
                            lastUpdated  = lastFetchedAt?.let { formatRelativeTime(it) },
                            refreshError = error
                        )
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState.LoadingNoCache)

    init { refresh() }

    // ac: transport-provider-breakdown: card responds to the period filter
    fun selectPeriod(p: Period) {
        _customFrom.value = null
        _customTo.value = null
        _period.value = p
        _refreshError.value = false
        refresh()
    }

    fun setCustomRange(from: String, to: String) {
        _customFrom.value = from
        _customTo.value = to
        _refreshError.value = false
        refresh()
    }

    fun refresh() = viewModelScope.launch {
        _isRefreshing.value = true
        _refreshError.value = false
        val result = useCase.refresh(_period.value, _customFrom.value, _customTo.value)
        if (result.isSuccess) lastFetchedAt = System.currentTimeMillis()
        _refreshError.value = result.isFailure
        _isRefreshing.value = false
    }

    fun monthDisplayLabel(ym: String): String {
        val (y, m) = ym.split("-").map { it.toInt() }
        val name = java.time.Month.of(m).getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
        return "$name ${(y % 100).toString().padStart(2, '0')}"
    }

    private fun formatRelativeTime(ms: Long): String {
        val mins = (System.currentTimeMillis() - ms) / 60_000
        return when {
            mins < 1L  -> "just now"
            mins == 1L -> "1 min ago"
            mins < 60L -> "$mins mins ago"
            else       -> "${mins / 60}h ago"
        }
    }
}
