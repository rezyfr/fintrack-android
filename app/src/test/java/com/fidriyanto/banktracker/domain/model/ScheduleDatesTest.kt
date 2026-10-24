package com.fidriyanto.banktracker.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ScheduleDatesTest {
    private fun s(repeat: ScheduleRepeat, every: Int, start: String, end: String? = null, through: String? = null) = Schedule(
        id = "1", name = "x", amount = 1.0, txType = "expense", walletId = "A", category = "c",
        repeat = repeat, every = every, startDate = start, endDate = end, postedThrough = through,
    )

    private fun d(v: String) = LocalDate.parse(v)

    @Test fun monthlyFallsOnTheDayEveryMonth() {
        val dates = ScheduleDates.between(s(ScheduleRepeat.MONTHLY, 1, "2026-11-01"), d("2026-11-01"), d("2027-01-31"))
        assertEquals(listOf(d("2026-11-01"), d("2026-12-01"), d("2027-01-01")), dates)
    }

    @Test fun day31UsesTheLastDayOfAShortMonth() {
        val dates = ScheduleDates.between(s(ScheduleRepeat.MONTHLY, 31, "2026-10-01"), d("2026-10-01"), d("2027-02-28"))
        assertEquals(listOf("2026-10-31", "2026-11-30", "2026-12-31", "2027-01-31", "2027-02-28"), dates.map { it.toString() })
    }

    @Test fun monthlyStartingAfterTheDayWaitsForNextMonth() {
        val dates = ScheduleDates.between(s(ScheduleRepeat.MONTHLY, 5, "2026-11-10"), d("2026-11-01"), d("2026-12-31"))
        assertEquals(listOf(d("2026-12-05")), dates)
    }

    @Test fun weeklyFallsOnTheWeekday() {
        // 2026-11-02 is a Monday
        val dates = ScheduleDates.between(s(ScheduleRepeat.WEEKLY, 3, "2026-11-02"), d("2026-11-01"), d("2026-11-20"))
        assertEquals(listOf("2026-11-04", "2026-11-11", "2026-11-18"), dates.map { it.toString() })
    }

    @Test fun everyNDaysCountsFromTheStartDate() {
        val dates = ScheduleDates.between(s(ScheduleRepeat.EVERY_N_DAYS, 10, "2026-11-01"), d("2026-11-05"), d("2026-12-01"))
        assertEquals(listOf("2026-11-11", "2026-11-21", "2026-12-01"), dates.map { it.toString() })
    }

    @Test fun theEndDateStopsTheSchedule() {
        val dates = ScheduleDates.between(s(ScheduleRepeat.MONTHLY, 1, "2026-11-01", end = "2026-12-15"), d("2026-11-01"), d("2027-06-01"))
        assertEquals(2, dates.size)
    }

    @Test fun dueSkipsWhatWasPostedAndIncludesMissedDates() {
        val sched = s(ScheduleRepeat.MONTHLY, 1, "2026-09-01", through = "2026-10-01")
        assertEquals(listOf("2026-11-01", "2026-12-01"), ScheduleDates.due(sched, d("2026-12-03")).map { it.toString() })
    }

    @Test fun nothingIsDueBeforeTheStartDate() {
        assertEquals(emptyList<LocalDate>(), ScheduleDates.due(s(ScheduleRepeat.MONTHLY, 1, "2026-12-01"), d("2026-11-20")))
    }

    @Test fun upcomingListsSoonestFirst() {
        val a = s(ScheduleRepeat.MONTHLY, 20, "2026-11-01")
        val b = s(ScheduleRepeat.MONTHLY, 5, "2026-11-01").copy(id = "2")
        val up = upcomingPayments(listOf(a, b), d("2026-11-01"), 30)
        assertEquals(listOf("2026-11-05", "2026-11-20"), up.map { it.date.toString() })
    }
}
