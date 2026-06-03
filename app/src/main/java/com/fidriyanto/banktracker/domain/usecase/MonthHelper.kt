package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.domain.model.Period
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

internal fun resolveMonths(period: Period, from: String?, to: String?): List<String> =
    if (from != null && to != null) monthsBetween(from, to) else monthsFor(period)

internal fun monthsFor(period: Period): List<String> {
    val now = LocalDate.now()
    return when (period) {
        Period.THIS_MONTH    -> listOf(monthLabel(now))
        Period.LAST_MONTH    -> listOf(monthLabel(now.minusMonths(1)))
        Period.LAST_3_MONTHS -> (1L..3L).map { monthLabel(now.minusMonths(it)) }
    }
}

internal fun monthsBetween(fromYM: String, toYM: String): List<String> {
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

internal fun monthLabel(date: LocalDate): String =
    "${date.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${date.year}"

internal fun monthsToDateRange(months: List<String>): Pair<String, String> {
    val fmt = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
    return YearMonth.parse(months.first(), fmt).atDay(1).toString() to
           YearMonth.parse(months.last(), fmt).atEndOfMonth().toString()
}

internal fun toYMPrefixes(months: List<String>): List<String> {
    val fmt = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
    return months.map { label ->
        val ym = YearMonth.parse(label, fmt)
        "${ym.year}-${ym.monthValue.toString().padStart(2, '0')}"
    }
}
