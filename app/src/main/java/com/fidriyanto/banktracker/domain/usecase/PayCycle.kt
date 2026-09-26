package com.fidriyanto.banktracker.domain.usecase

import java.time.LocalDate
import java.time.format.DateTimeFormatter

// Pay cycle runs from the 26th through the 25th of the next month, matching the salary date.
object PayCycle {
    const val START_DAY = 26
    private val iso = DateTimeFormatter.ISO_LOCAL_DATE

    data class Range(val fromIso: String, val toIso: String, val start: LocalDate, val end: LocalDate)

    fun current(today: LocalDate = LocalDate.now()): Range {
        val start = if (today.dayOfMonth >= START_DAY) today.withDayOfMonth(START_DAY)
                    else today.minusMonths(1).withDayOfMonth(START_DAY)
        val end = start.plusMonths(1).minusDays(1)
        return Range(start.format(iso), end.format(iso), start, end)
    }

    // Days until the next payday (the next 26th on or after today). On the 26th this is 0 (today).
    fun daysToPayday(today: LocalDate = LocalDate.now()): Int {
        val next = if (today.dayOfMonth <= START_DAY) today.withDayOfMonth(START_DAY)
                   else today.plusMonths(1).withDayOfMonth(START_DAY)
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
