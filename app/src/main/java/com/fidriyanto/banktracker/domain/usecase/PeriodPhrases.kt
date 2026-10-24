package com.fidriyanto.banktracker.domain.usecase

import java.time.LocalDate
import java.time.YearMonth

// Reads a date phrase the way a person says it, such as "first week of August 2026". The model passes the
// phrase on as it came. This code turns it into exact dates, because a small model gets dates wrong.
// A week of a month is a block of days: the 1st to 7th, 8th to 14th, 15th to 21st, 22nd to 28th, and the last week
// runs from the 22nd to the end of the month. A month without a year means its latest past occurrence.
object PeriodPhrases {
    const val EXAMPLES = "first week of August 2026, August, Q3 2026, last 3 weeks, 3 to 9 August, this year"

    private val months = listOf("january", "february", "march", "april", "may", "june", "july", "august", "september", "october", "november", "december")
    private val short = mapOf("jan" to 1, "feb" to 2, "mar" to 3, "apr" to 4, "jun" to 6, "jul" to 7, "aug" to 8, "sep" to 9, "sept" to 9, "oct" to 10, "nov" to 11, "dec" to 12)
    private val ordinals = mapOf("first" to 1, "1st" to 1, "second" to 2, "2nd" to 2, "third" to 3, "3rd" to 3, "fourth" to 4, "4th" to 4, "fifth" to 5, "5th" to 5, "last" to -1)

    private val MON = "(" + (months + short.keys).joinToString("|") + ")"
    private val MONTH_REF = "($MON(?: (\\d{4}))?|this month|last month|current month)"
    private val DAY = "(\\d{1,2})(?:st|nd|rd|th)?"
    private val TO = "(?: to | - |-| until | through )"
    private val ORD = "(first|1st|second|2nd|third|3rd|fourth|4th|fifth|5th|last)"

    fun parse(phrase: String, today: LocalDate = LocalDate.now()): PeriodResolver.Span? {
        val t = phrase.lowercase().replace('_', ' ').replace(',', ' ').replace(Regex("\\b(the|in|during|on|of|for|within|from)\\b"), " ").replace(Regex("\\s+"), " ").trim()
        if (t.isEmpty()) return null
        return weekOfMonth(t, today) ?: half(t, today) ?: dayRange(t, today) ?: singleDay(t, today) ?: quarter(t, today) ?: recent(t, today)
            ?: year(t, today) ?: iso(t) ?: month(t, today)?.let { span(it.atDay(1), it.atEndOfMonth()) }
    }

    private fun span(a: LocalDate, b: LocalDate) = PeriodResolver.Span(a, b)

    private fun monthNumber(name: String): Int = months.indexOf(name).let { if (it >= 0) it + 1 else short[name] ?: 0 }

    // A month name with an optional year, or this month or last month. Without a year it is the latest one that is not in the future.
    private fun monthFrom(name: String?, year: String?, ref: String?, today: LocalDate): YearMonth? {
        val now = YearMonth.from(today)
        return when {
            ref == "last month" -> now.minusMonths(1)
            ref == "this month" || ref == "current month" -> now
            name != null -> {
                val m = monthNumber(name)
                val y = year?.toIntOrNull() ?: (if (m > today.monthValue) today.year - 1 else today.year)
                YearMonth.of(y, m)
            }
            else -> null
        }
    }

    private fun month(t: String, today: LocalDate): YearMonth? =
        Regex("^$MONTH_REF$").find(t)?.let { monthFrom(it.groupValues[2].ifEmpty { null }, it.groupValues[3].ifEmpty { null }, it.groupValues[1].takeIf { r -> r.endsWith("month") }, today) }

    private fun weekOfMonth(t: String, today: LocalDate): PeriodResolver.Span? {
        val m = Regex("^$ORD week $MONTH_REF$").find(t) ?: Regex("^week (\\d) $MONTH_REF$").find(t)?.let { return numberedWeek(it, today) } ?: return null
        val ym = monthFrom(m.groupValues[3].ifEmpty { null }, m.groupValues[4].ifEmpty { null }, m.groupValues[2].takeIf { it.endsWith("month") }, today) ?: return null
        val n = ordinals[m.groupValues[1]] ?: return null
        return block(ym, n)
    }

    private fun numberedWeek(m: MatchResult, today: LocalDate): PeriodResolver.Span? {
        val ym = monthFrom(m.groupValues[3].ifEmpty { null }, m.groupValues[4].ifEmpty { null }, m.groupValues[2].takeIf { it.endsWith("month") }, today) ?: return null
        return block(ym, m.groupValues[1].toInt())
    }

    // The nth block of seven days. -1 is the last week: the 22nd to the end of the month.
    private fun block(ym: YearMonth, n: Int): PeriodResolver.Span? {
        if (n == -1) return span(ym.atDay(22), ym.atEndOfMonth())
        val start = (n - 1) * 7 + 1
        if (n !in 1..5 || start > ym.lengthOfMonth()) return null
        return span(ym.atDay(start), ym.atDay(minOf(start + 6, ym.lengthOfMonth())))
    }

    private fun half(t: String, today: LocalDate): PeriodResolver.Span? {
        val m = Regex("^(first|1st|second|2nd) half $MONTH_REF$").find(t) ?: return null
        val ym = monthFrom(m.groupValues[3].ifEmpty { null }, m.groupValues[4].ifEmpty { null }, m.groupValues[2].takeIf { it.endsWith("month") }, today) ?: return null
        return if (m.groupValues[1].startsWith("f") || m.groupValues[1] == "1st") span(ym.atDay(1), ym.atDay(15)) else span(ym.atDay(16), ym.atEndOfMonth())
    }

    private fun dayRange(t: String, today: LocalDate): PeriodResolver.Span? {
        // 3 to 9 august [2026]
        Regex("^$DAY$TO$DAY $MON(?: (\\d{4}))?$").find(t)?.let { m ->
            val ym = monthFrom(m.groupValues[3], m.groupValues[4].ifEmpty { null }, null, today) ?: return null
            return days(ym, m.groupValues[1].toInt(), ym, m.groupValues[2].toInt())
        }
        // august 3 to 9 [2026]
        Regex("^$MON $DAY$TO$DAY(?: (\\d{4}))?$").find(t)?.let { m ->
            val ym = monthFrom(m.groupValues[1], m.groupValues[4].ifEmpty { null }, null, today) ?: return null
            return days(ym, m.groupValues[2].toInt(), ym, m.groupValues[3].toInt())
        }
        // 3 august to 9 september [2026]  or  august 3 to september 9 [2026]
        val full = Regex("^$DAY $MON$TO$DAY $MON(?: (\\d{4}))?$").find(t)
        if (full != null) {
            val g = full.groupValues
            return days(monthFrom(g[2], g[5].ifEmpty { null }, null, today) ?: return null, g[1].toInt(), monthFrom(g[4], g[5].ifEmpty { null }, null, today) ?: return null, g[3].toInt())
        }
        val alt = Regex("^$MON $DAY$TO$MON $DAY(?: (\\d{4}))?$").find(t) ?: return null
        val g = alt.groupValues
        return days(monthFrom(g[1], g[5].ifEmpty { null }, null, today) ?: return null, g[2].toInt(), monthFrom(g[3], g[5].ifEmpty { null }, null, today) ?: return null, g[4].toInt())
    }

    private fun days(a: YearMonth, da: Int, b: YearMonth, db: Int): PeriodResolver.Span? {
        if (da !in 1..a.lengthOfMonth() || db !in 1..b.lengthOfMonth()) return null
        val x = a.atDay(da)
        val y = b.atDay(db)
        return if (x <= y) span(x, y) else span(y, x)
    }

    private fun singleDay(t: String, today: LocalDate): PeriodResolver.Span? {
        val m = Regex("^$DAY $MON(?: (\\d{4}))?$").find(t)?.let { Triple(it.groupValues[1], it.groupValues[2], it.groupValues[3]) }
            ?: Regex("^$MON $DAY(?: (\\d{4}))?$").find(t)?.let { Triple(it.groupValues[2], it.groupValues[1], it.groupValues[3]) } ?: return null
        val ym = monthFrom(m.second, m.third.ifEmpty { null }, null, today) ?: return null
        return days(ym, m.first.toInt(), ym, m.first.toInt())
    }

    private fun quarter(t: String, today: LocalDate): PeriodResolver.Span? {
        val m = Regex("^q([1-4])(?: (\\d{4}))?$").find(t) ?: return null
        val q = m.groupValues[1].toInt()
        val first = (q - 1) * 3 + 1
        val year = m.groupValues[2].toIntOrNull() ?: (if (first > today.monthValue) today.year - 1 else today.year)
        return span(LocalDate.of(year, first, 1), YearMonth.of(year, first + 2).atEndOfMonth())
    }

    private fun recent(t: String, today: LocalDate): PeriodResolver.Span? {
        val m = Regex("^(?:last|past|previous) (\\d{1,3}) (day|week|month)s?$").find(t) ?: return null
        val n = m.groupValues[1].toInt().takeIf { it > 0 } ?: return null
        val from = when (m.groupValues[2]) {
            "day" -> today.minusDays(n - 1L)
            "week" -> today.minusDays(n * 7L - 1)
            else -> today.minusMonths(n.toLong()).plusDays(1)
        }
        return span(from, today)
    }

    private fun year(t: String, today: LocalDate): PeriodResolver.Span? = when {
        t == "this year" || t == "year to date" || t == "ytd" -> span(LocalDate.of(today.year, 1, 1), today)
        t == "last year" -> span(LocalDate.of(today.year - 1, 1, 1), LocalDate.of(today.year - 1, 12, 31))
        Regex("^\\d{4}$").matches(t) -> t.toInt().let { span(LocalDate.of(it, 1, 1), LocalDate.of(it, 12, 31)) }
        else -> null
    }

    private fun iso(t: String): PeriodResolver.Span? {
        Regex("^(\\d{4})-(\\d{2})$").find(t)?.let { m -> runCatching { YearMonth.of(m.groupValues[1].toInt(), m.groupValues[2].toInt()) }.getOrNull()?.let { return span(it.atDay(1), it.atEndOfMonth()) } }
        Regex("^\\d{4}-\\d{2}-\\d{2}$").find(t)?.let { m -> runCatching { LocalDate.parse(m.value) }.getOrNull()?.let { return span(it, it) } }
        return null
    }
}
