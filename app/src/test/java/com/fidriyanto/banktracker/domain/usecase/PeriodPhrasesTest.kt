package com.fidriyanto.banktracker.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class PeriodPhrasesTest {
    // A Monday in the middle of October 2026.
    private val today = LocalDate.of(2026, 10, 12)

    private fun r(phrase: String): String? = PeriodResolver.resolve(phrase, null, null, today)?.let { "${it.from} to ${it.to}" }

    @Test fun a_week_of_a_month_is_a_block_of_seven_days() {
        assertEquals("2026-08-01 to 2026-08-07", r("first week of August 2026"))
        assertEquals("2026-08-08 to 2026-08-14", r("second week of august"))
        assertEquals("2026-08-15 to 2026-08-21", r("3rd week in August"))
        assertEquals("2026-02-22 to 2026-02-28", r("fourth week of february 2026"))
        assertEquals("2026-08-29 to 2026-08-31", r("fifth week of August 2026"))
        assertNull(r("fifth week of February 2026"))
        assertEquals("2026-08-22 to 2026-08-31", r("last week of August 2026"))
        assertEquals("2026-08-08 to 2026-08-14", r("week 2 of august"))
    }

    @Test fun a_month_without_a_year_is_its_latest_one_that_is_not_in_the_future() {
        assertEquals("2026-08-01 to 2026-08-31", r("August"))
        assertEquals("2026-08-01 to 2026-08-31", r("in Aug"))
        assertEquals("2025-12-01 to 2025-12-31", r("december"))
        assertEquals("2026-10-01 to 2026-10-31", r("october"))
        assertEquals("2024-08-01 to 2024-08-31", r("August 2024"))
        assertEquals("2026-08-01 to 2026-08-31", r("2026-08"))
    }

    @Test fun this_month_and_last_month_work_inside_a_phrase() {
        assertEquals("2026-09-01 to 2026-09-07", r("first week of last month"))
        assertEquals("2026-10-08 to 2026-10-14", r("second week of this month"))
        assertEquals("2026-09-16 to 2026-09-30", r("second half of last month"))
    }

    @Test fun a_half_of_a_month() {
        assertEquals("2026-09-01 to 2026-09-15", r("first half of September"))
        assertEquals("2026-09-16 to 2026-09-30", r("second half of September 2026"))
    }

    @Test fun a_quarter_a_year_and_year_to_date() {
        assertEquals("2026-07-01 to 2026-09-30", r("Q3 2026"))
        assertEquals("2026-01-01 to 2026-03-31", r("q1"))
        assertEquals("2026-10-01 to 2026-12-31", r("q4"))
        assertEquals("2025-01-01 to 2025-12-31", r("last year"))
        assertEquals("2026-01-01 to 2026-10-12", r("this year"))
        assertEquals("2026-01-01 to 2026-10-12", r("year to date"))
        assertEquals("2025-01-01 to 2025-12-31", r("2025"))
    }

    @Test fun the_last_few_days_weeks_or_months() {
        assertEquals("2026-10-03 to 2026-10-12", r("past 10 days"))
        assertEquals("2026-09-22 to 2026-10-12", r("last 3 weeks"))
        assertEquals("2026-08-13 to 2026-10-12", r("last 2 months"))
    }

    @Test fun a_range_of_days_and_a_single_day() {
        assertEquals("2026-08-03 to 2026-08-09", r("3 to 9 August"))
        assertEquals("2026-08-03 to 2026-08-09", r("3-9 august 2026"))
        assertEquals("2026-08-03 to 2026-08-09", r("August 3 to 9"))
        assertEquals("2026-08-03 to 2026-08-09", r("3 august to 9 august"))
        assertEquals("2026-08-30 to 2026-09-05", r("30 August to 5 September"))
        assertEquals("2026-08-05 to 2026-08-05", r("5 august 2026"))
        assertEquals("2026-08-05 to 2026-08-05", r("august 5th"))
        assertEquals("2026-08-05 to 2026-08-05", r("2026-08-05"))
        assertNull(r("31 June"))
    }

    @Test fun the_old_period_words_and_exact_dates_still_work() {
        assertEquals("2026-10-11 to 2026-10-11", r("yesterday"))
        assertEquals("2026-09-01 to 2026-09-30", r("last_month"))
        assertEquals("2026-10-01 to 2026-10-12", r("this-month"))
        assertEquals("2026-08-01 to 2026-08-07", PeriodResolver.resolve("last_month", "2026-08-01", "2026-08-07", today)?.let { "${it.from} to ${it.to}" })
    }

    @Test fun a_phrase_nobody_can_read_gives_nothing() {
        assertNull(r("banana"))
        assertNull(r("sometime soon"))
        assertNull(r("week 9 of august"))
    }
}
