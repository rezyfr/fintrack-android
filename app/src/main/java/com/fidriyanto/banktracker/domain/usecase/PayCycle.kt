package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.domain.model.LEGACY_CYCLE_START_DAY
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// A pay cycle runs from its start day through the day before the next start day. With start day 1 it is a calendar month.
object PayCycle {
    // The day each cycle starts. The settings store sets it, and an installation that never chose one keeps 26.
    @Volatile var startDay: Int = LEGACY_CYCLE_START_DAY
    private val iso = DateTimeFormatter.ISO_LOCAL_DATE

    data class Range(val fromIso: String, val toIso: String, val start: LocalDate, val end: LocalDate)

    fun current(today: LocalDate = LocalDate.now()): Range = cycleRange(0, today)

    // ac: insights-pay-cycle-periods — offset 0 is the cycle in progress, 1 the one before it, and so
    // on, mirroring web/src/utils/payCycle.js so both platforms agree on where a cycle begins and ends.
    fun cycleRange(offset: Int, today: LocalDate = LocalDate.now(), startDay: Int = this.startDay): Range {
        val monthsBack = offset + (if (today.dayOfMonth >= startDay) 0 else 1)
        val start = today.minusMonths(monthsBack.toLong()).withDayOfMonth(startDay)
        val end = start.plusMonths(1).minusDays(1)
        return Range(start.format(iso), end.format(iso), start, end)
    }

    // Days until the next payday (the next 26th on or after today). On the 26th this is 0 (today).
    fun daysToPayday(today: LocalDate = LocalDate.now()): Int {
        val next = if (today.dayOfMonth <= startDay) today.withDayOfMonth(startDay)
        else today.plusMonths(1).withDayOfMonth(startDay)
        return (next.toEpochDay() - today.toEpochDay()).toInt()
    }
}

// A weekday-aligned month grid (Sunday-first) for the pay cycle containing [anyIso], padded to
// whole weeks. Each cell carries its date and whether it falls inside the cycle.
object CalendarGrid {
    data class Cell(val iso: String, val inCycle: Boolean)

    fun weeksForCycle(range: PayCycle.Range): List<List<Cell>> {
        val iso = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE
        val inCycle = HashSet<String>()
        run {
            var d = range.start
            while (!d.isAfter(range.end)) { inCycle.add(d.format(iso)); d = d.plusDays(1) }
        }
        // back to the Sunday on/before the start (DayOfWeek: Mon=1..Sun=7)
        var gridStart = range.start
        while (gridStart.dayOfWeek.value != 7) gridStart = gridStart.minusDays(1)
        var gridEnd = range.end
        while (gridEnd.dayOfWeek.value != 6) gridEnd = gridEnd.plusDays(1)
        val weeks = ArrayList<List<Cell>>()
        var week = ArrayList<Cell>()
        var d = gridStart
        while (!d.isAfter(gridEnd)) {
            val s = d.format(iso)
            week.add(Cell(s, inCycle.contains(s)))
            if (week.size == 7) { weeks.add(week); week = ArrayList() }
            d = d.plusDays(1)
        }
        if (week.isNotEmpty()) weeks.add(week)
        return weeks
    }
}
