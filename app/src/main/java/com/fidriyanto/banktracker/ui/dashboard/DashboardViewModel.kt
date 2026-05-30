package com.fidriyanto.banktracker.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fidriyanto.banktracker.data.db.MerchantTotal
import com.fidriyanto.banktracker.data.db.MonthlyOverviewEntity
import com.fidriyanto.banktracker.domain.usecase.GetDashboardDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val useCase: GetDashboardDataUseCase
) : ViewModel() {

    private val _period      = MutableStateFlow(Period.THIS_MONTH)
    private val _customFrom  = MutableStateFlow<String?>(null) // "YYYY-MM"
    private val _customTo    = MutableStateFlow<String?>(null) // "YYYY-MM"
    private val _isRefreshing = MutableStateFlow(false)
    private val _refreshError = MutableStateFlow(false)
    @Volatile private var lastFetchedAt: Long? = null

    val period     = _period.asStateFlow()
    val customFrom = _customFrom.asStateFlow()
    val customTo   = _customTo.asStateFlow()
    val isCustom   = combine(_customFrom, _customTo) { f, t -> f != null && t != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // 24 months back from now, for the custom picker dropdown.
    val availableMonths: List<String> = (0..23).map { i ->
        val d = LocalDate.now().minusMonths(i.toLong())
        "${d.year}-${d.monthValue.toString().padStart(2, '0')}"
    }

    val state: StateFlow<DashboardUiState> =
        combine(_period, _customFrom, _customTo) { p, from, to -> Triple(p, from, to) }
            .flatMapLatest { (p, from, to) ->
                val months = resolveMonths(p, from, to)
                val (fromDate, toDate) = monthsToDateRange(months)
                combine(
                    useCase.observeForMonths(months),
                    useCase.observeTransportBreakdown(fromDate, toDate),
                    _isRefreshing,
                    _refreshError
                ) { rows, transport, refreshing, error ->
                    val thbRows = rows.first
                    val idrRows = rows.second
                    if (thbRows.isEmpty() && idrRows.isEmpty() && !refreshing && !error) {
                        DashboardUiState.LoadingNoCache
                    } else {
                        DashboardUiState.Loaded(
                            period = p,
                            thb = buildSummary(thbRows, transport.first),
                            idr = buildSummary(idrRows, transport.second),
                            isRefreshing = refreshing,
                            lastUpdated = lastFetchedAt?.let { formatRelativeTime(it) },
                            refreshError = error
                        )
                    }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState.LoadingNoCache)

    init { refresh() }

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
        val months = resolveMonths(_period.value, _customFrom.value, _customTo.value)
        val result = useCase.refresh(months)
        if (result.isSuccess) lastFetchedAt = System.currentTimeMillis()
        _refreshError.value = result.isFailure
        _isRefreshing.value = false
    }

    private fun resolveMonths(period: Period, from: String?, to: String?): List<String> =
        if (from != null && to != null) monthsBetween(from, to)
        else monthsFor(period)

    private fun monthsFor(period: Period): List<String> {
        val now = LocalDate.now()
        return when (period) {
            Period.THIS_MONTH    -> listOf(monthLabel(now))
            Period.LAST_MONTH    -> listOf(monthLabel(now.minusMonths(1)))
            Period.LAST_3_MONTHS -> (1L..3L).map { monthLabel(now.minusMonths(it)) }
        }
    }

    private fun monthsBetween(fromYM: String, toYM: String): List<String> {
        val (fy, fm) = fromYM.split("-").map { it.toInt() }
        val (ty, tm) = toYM.split("-").map { it.toInt() }
        val result = mutableListOf<String>()
        var y = fy; var m = fm
        while (y < ty || (y == ty && m <= tm)) {
            result += monthLabel(LocalDate.of(y, m, 1))
            m++; if (m > 12) { m = 1; y++ }
        }
        return result.ifEmpty { listOf(monthLabel(LocalDate.now())) }
    }

    fun monthLabel(date: LocalDate): String {
        val name = date.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
        return "$name ${date.year}"
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

    private fun monthsToDateRange(months: List<String>): Pair<String, String> {
        val fmt = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
        val first = YearMonth.parse(months.first(), fmt).atDay(1)
        val last = YearMonth.parse(months.last(), fmt).atEndOfMonth()
        return first.toString() to last.toString()
    }

    private fun buildTransportBreakdown(merchants: List<MerchantTotal>, topN: Int = 3): List<MerchantRow> {
        val total = merchants.sumOf { it.amount }
        if (total == 0.0) return emptyList()
        val top = merchants.take(topN)
        val otherAmount = merchants.drop(topN).sumOf { it.amount }
        val rows = top.map { MerchantRow(it.merchant, it.amount, (it.amount / total).toFloat()) }
        return if (otherAmount > 0.0) rows + MerchantRow("Other", otherAmount, (otherAmount / total).toFloat())
        else rows
    }

    private fun buildSummary(rows: List<MonthlyOverviewEntity>, transportMerchants: List<MerchantTotal> = emptyList()): CurrencySummary {
        val totalIncome = rows.sumOf { it.income }
        val totalExpenses = rows.sumOf { it.totalExpenditure }
        val net = totalIncome - totalExpenses
        val breakdown = listOf(
            "Bills"              to rows.sumOf { it.bills },
            "Subscriptions"      to rows.sumOf { it.subscriptions },
            "Entertainment"      to rows.sumOf { it.entertainment },
            "Food & Drink"       to rows.sumOf { it.foodDrink },
            "Groceries"          to rows.sumOf { it.groceries },
            "Health & Wellbeing" to rows.sumOf { it.healthWellbeing },
            "Family"             to rows.sumOf { it.family },
            "Other"              to rows.sumOf { it.other },
            "Shopping"           to rows.sumOf { it.shopping },
            "Transport"          to rows.sumOf { it.transport },
            "Travel"             to rows.sumOf { it.travel },
            "Business"           to rows.sumOf { it.business },
            "Gifts"              to rows.sumOf { it.gifts },
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
        return CurrencySummary(totalIncome, totalExpenses, net, breakdown, buildTransportBreakdown(transportMerchants))
    }
}
