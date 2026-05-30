package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.repository.DashboardRepository
import com.fidriyanto.banktracker.domain.model.DashboardSummaryPair
import com.fidriyanto.banktracker.domain.model.Period
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetDashboardDataUseCase @Inject constructor(
    private val repository: DashboardRepository
) {
    fun observe(period: Period, customFrom: String?, customTo: String?): Flow<DashboardSummaryPair> {
        val months = resolveMonths(period, customFrom, customTo)
        val (fromDate, toDate) = monthsToDateRange(months)
        return combine(
            repository.observeForMonths(months),
            repository.observeTransportBreakdown(fromDate, toDate)
        ) { rows, transport ->
            DashboardSummaryPair(
                thb = buildSummary(rows.first, transport.first),
                idr = buildSummary(rows.second, transport.second)
            )
        }
    }

    suspend fun refresh(period: Period, customFrom: String?, customTo: String?): Result<Unit> =
        repository.refresh(resolveMonths(period, customFrom, customTo))

    private fun resolveMonths(period: Period, from: String?, to: String?): List<String> =
        if (from != null && to != null) monthsBetween(from, to) else monthsFor(period)

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

    private fun monthLabel(date: LocalDate) =
        "${date.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${date.year}"

    private fun monthsToDateRange(months: List<String>): Pair<String, String> {
        val fmt = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
        return YearMonth.parse(months.first(), fmt).atDay(1).toString() to
               YearMonth.parse(months.last(), fmt).atEndOfMonth().toString()
    }
}
