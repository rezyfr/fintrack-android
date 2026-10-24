package com.fidriyanto.banktracker.domain.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

// ac: scheduled-transactions - the dates a schedule falls on
object ScheduleDates {
    // Every date of [s] from [from] to [to], both included, between its start and end dates.
    // A month with fewer days than [Schedule.every] uses its last day.
    fun between(s: Schedule, from: LocalDate, to: LocalDate): List<LocalDate> {
        val start = LocalDate.parse(s.startDate)
        val upper = s.endDate?.let { minOf(to, LocalDate.parse(it)) } ?: to
        val lower = maxOf(from, start)
        if (lower.isAfter(upper)) return emptyList()
        val all = when (s.repeat) {
            ScheduleRepeat.MONTHLY -> monthly(s.every, start, upper)
            ScheduleRepeat.WEEKLY -> stepped(firstWeekday(start, s.every), 7, upper)
            ScheduleRepeat.EVERY_N_DAYS -> stepped(start, s.every.coerceAtLeast(1), upper)
        }
        return all.filter { !it.isBefore(lower) }
    }

    // The dates the app still has to post up to [today].
    fun due(s: Schedule, today: LocalDate): List<LocalDate> {
        val after = s.postedThrough?.let { LocalDate.parse(it).plusDays(1) } ?: LocalDate.parse(s.startDate)
        return between(s, after, today)
    }

    private fun monthly(day: Int, start: LocalDate, upper: LocalDate): List<LocalDate> {
        val out = ArrayList<LocalDate>()
        var month = YearMonth.from(start)
        while (!month.atDay(1).isAfter(upper)) {
            val date = month.atDay(day.coerceIn(1, month.lengthOfMonth()))
            if (!date.isBefore(start) && !date.isAfter(upper)) out.add(date)
            month = month.plusMonths(1)
        }
        return out
    }

    private fun firstWeekday(start: LocalDate, weekday: Int): LocalDate {
        val target = DayOfWeek.of(weekday.coerceIn(1, 7))
        return start.plusDays(((target.value - start.dayOfWeek.value + 7) % 7).toLong())
    }

    private fun stepped(first: LocalDate, days: Int, upper: LocalDate): List<LocalDate> =
        generateSequence(first) { it.plusDays(days.toLong()) }.takeWhile { !it.isAfter(upper) }.toList()
}

// ac: scheduled-transactions - one date a schedule falls on in the coming days
data class UpcomingPayment(val date: LocalDate, val schedule: Schedule)

// The dates of every schedule from [today] for [days] days, soonest first. A schedule that is off still shows.
fun upcomingPayments(schedules: List<Schedule>, today: LocalDate, days: Int): List<UpcomingPayment> =
    schedules.flatMap { s -> ScheduleDates.between(s, today, today.plusDays(days.toLong())).map { UpcomingPayment(it, s) } }
        .sortedBy { it.date }
