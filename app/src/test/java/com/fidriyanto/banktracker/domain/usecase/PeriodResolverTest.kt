package com.fidriyanto.banktracker.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class PeriodResolverTest {
    // 2026-10-11 is a Sunday, inside the cycle that began on 2026-09-26.
    private val today = LocalDate.of(2026, 10, 11)

    private fun resolve(p: String?, from: String? = null, to: String? = null) = PeriodResolver.resolve(p, from, to, today)

    @Test fun weeks_run_monday_to_sunday() {
        assertEquals(PeriodResolver.Span(LocalDate.of(2026, 10, 5), today), resolve("this_week"))
        assertEquals(PeriodResolver.Span(LocalDate.of(2026, 9, 28), LocalDate.of(2026, 10, 4)), resolve("last_week"))
    }

    @Test fun months() {
        assertEquals(PeriodResolver.Span(LocalDate.of(2026, 10, 1), today), resolve("this_month"))
        assertEquals(PeriodResolver.Span(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)), resolve("last_month"))
    }

    @Test fun pay_cycles() {
        assertEquals(PeriodResolver.Span(LocalDate.of(2026, 9, 26), today), resolve("this_cycle"))
        assertEquals(PeriodResolver.Span(LocalDate.of(2026, 8, 26), LocalDate.of(2026, 9, 25)), resolve("last_cycle"))
        assertEquals(resolve("this_cycle"), resolve(null))
    }

    @Test fun rolling_days_and_spelling_variants() {
        assertEquals(PeriodResolver.Span(LocalDate.of(2026, 10, 5), today), resolve("Last 7 days"))
        assertEquals(PeriodResolver.Span(today.minusDays(1), today.minusDays(1)), resolve("yesterday"))
    }

    @Test fun explicit_dates_win_and_are_ordered() {
        assertEquals(
            PeriodResolver.Span(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 3)),
            resolve("last_week", "2026-10-03", "2026-10-01"),
        )
    }

    @Test fun unknown_period_is_null() {
        assertNull(resolve("next_century"))
    }
}
