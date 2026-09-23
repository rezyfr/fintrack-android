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

    // Days until the next payday (the next 26th on or after today).
    fun daysToPayday(today: LocalDate = LocalDate.now()): Int {
        val next = if (today.dayOfMonth < START_DAY) today.withDayOfMonth(START_DAY)
                   else today.plusMonths(1).withDayOfMonth(START_DAY)
        return (next.toEpochDay() - today.toEpochDay()).toInt()
    }
}
