package com.fidriyanto.banktracker.domain.usecase

import java.time.DayOfWeek
import java.time.LocalDate

// Turns a period word from the model into dates. The model never writes dates on its own, because
// a small model gets weeks, months and the pay cycle wrong.
object PeriodResolver {
    const val KEYWORDS =
        "today, yesterday, this_week, last_week, last_7_days, last_30_days, last_90_days, this_month, last_month, this_cycle, last_cycle"

    data class Span(val from: LocalDate, val to: LocalDate)

    // Explicit from and to dates win. With neither a period nor dates, the answer is the cycle in progress.
    fun resolve(period: String?, from: String?, to: String?, today: LocalDate = LocalDate.now()): Span? {
        val a = from?.let(::parse)
        val b = to?.let(::parse)
        if (a != null && b != null) return if (a <= b) Span(a, b) else Span(b, a)
        val monday = today.with(DayOfWeek.MONDAY)
        val lastMonthStart = today.withDayOfMonth(1).minusMonths(1)
        return when (period.orEmpty().trim().lowercase().replace(' ', '_').replace('-', '_')) {
            "", "this_cycle" -> cycle(0, today)
            "last_cycle" -> cycle(1, today)
            "today" -> Span(today, today)
            "yesterday" -> Span(today.minusDays(1), today.minusDays(1))
            "this_week" -> Span(monday, today)
            "last_week" -> Span(monday.minusDays(7), monday.minusDays(1))
            "last_7_days" -> Span(today.minusDays(6), today)
            "last_30_days" -> Span(today.minusDays(29), today)
            "last_90_days" -> Span(today.minusDays(89), today)
            "this_month" -> Span(today.withDayOfMonth(1), today)
            "last_month" -> Span(lastMonthStart, lastMonthStart.withDayOfMonth(lastMonthStart.lengthOfMonth()))
            // Anything else is a phrase, such as "first week of August 2026".
            else -> PeriodPhrases.parse(period.orEmpty(), today)
        }
    }

    private fun cycle(offset: Int, today: LocalDate): Span {
        val c = PayCycle.cycleRange(offset, today)
        return Span(c.start, if (c.end.isAfter(today)) today else c.end)
    }

    private fun parse(s: String): LocalDate? = runCatching { LocalDate.parse(s.trim()) }.getOrNull()
}
