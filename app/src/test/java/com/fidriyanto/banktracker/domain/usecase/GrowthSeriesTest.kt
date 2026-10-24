package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.domain.model.AccountType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class GrowthSeriesTest {
    private fun dates(n: Int): List<LocalDate> = (0 until n).map { LocalDate.of(2026, 1, 25).plusMonths(it.toLong()) }

    private fun history(n: Int, vararg accounts: AccountHistory) = GrowthHistory(dates(n), accounts.toList())

    private fun acc(id: String, type: AccountType, vararg values: Double, currency: String = "USD") =
        AccountHistory(id, currency, type, values.toList())

    // ac: account-growth-chart — checking and investment are stacked, and a credit card is not counted
    @Test fun stacks_checking_and_investment_and_leaves_out_credit() {
        val h = history(
            3,
            acc("A", AccountType.CHECKING, 100.0, 150.0, 200.0),
            acc("B", AccountType.CHECKING, 10.0, 10.0, 20.0),
            acc("I", AccountType.INVESTMENT, 50.0, 60.0, 70.0),
            acc("C", AccountType.CREDIT, -500.0, -800.0, -900.0),
        )
        val s = GrowthSeriesBuilder.build(h, "USD", GrowthRange.ALL)!!
        assertEquals(listOf(110.0, 160.0, 220.0), s.points.map { it.checking })
        assertEquals(listOf(50.0, 60.0, 70.0), s.points.map { it.investment })
        assertEquals(listOf(160.0, 220.0, 290.0), s.points.map { it.total })
    }

    @Test fun the_last_point_is_today() {
        val h = history(3, acc("A", AccountType.CHECKING, 1.0, 2.0, 3.0))
        val s = GrowthSeriesBuilder.build(h, "USD", GrowthRange.ALL)!!
        assertEquals(listOf(false, false, true), s.points.map { it.isNow })
    }

    // ac: account-growth-chart — the leading points before the first change are cut, and the last of
    // them stays as the level the chart starts from
    @Test fun cuts_the_run_before_the_first_change() {
        val h = history(5, acc("A", AccountType.CHECKING, 100.0, 100.0, 100.0, 130.0, 140.0))
        val s = GrowthSeriesBuilder.build(h, "USD", GrowthRange.ALL)!!
        assertEquals(listOf(100.0, 130.0, 140.0), s.points.map { it.total })
        assertEquals(h.dates[2], s.points.first().date)
    }

    @Test fun a_range_keeps_the_latest_points_counting_today() {
        val h = history(10, acc("A", AccountType.CHECKING, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0))
        val s = GrowthSeriesBuilder.build(h, "USD", GrowthRange.SIX_MONTHS)!!
        assertEquals(listOf(5.0, 6.0, 7.0, 8.0, 9.0, 10.0), s.points.map { it.total })
        assertTrue(s.points.last().isNow)
    }

    @Test fun nothing_to_draw_when_no_account_ever_changed() {
        val h = history(4, acc("A", AccountType.CHECKING, 0.0, 0.0, 0.0, 0.0))
        assertNull(GrowthSeriesBuilder.build(h, "USD", GrowthRange.ALL))
    }

    @Test fun nothing_to_draw_for_a_currency_with_no_account() {
        val h = history(3, acc("A", AccountType.CHECKING, 1.0, 2.0, 3.0))
        assertNull(GrowthSeriesBuilder.build(h, "EUR", GrowthRange.ALL))
    }

    @Test fun only_the_accounts_of_the_chosen_book_count() {
        val h = history(
            3,
            acc("A", AccountType.CHECKING, 1.0, 2.0, 3.0),
            acc("B", AccountType.CHECKING, 10.0, 20.0, 30.0),
        )
        val s = GrowthSeriesBuilder.build(h, "USD", GrowthRange.ALL, include = { it == "A" })!!
        assertEquals(listOf(1.0, 2.0, 3.0), s.points.map { it.total })
    }

    @Test fun lists_each_currency_once_and_skips_credit_only_currencies() {
        val h = history(
            2,
            acc("A", AccountType.CHECKING, 1.0, 2.0),
            acc("B", AccountType.INVESTMENT, 1.0, 2.0),
            acc("E", AccountType.CHECKING, 1.0, 2.0, currency = "EUR"),
            acc("C", AccountType.CREDIT, 1.0, 2.0, currency = "JPY"),
        )
        assertEquals(listOf("USD", "EUR"), GrowthSeriesBuilder.currencies(h))
        assertFalse(GrowthSeriesBuilder.currencies(h).contains("JPY"))
    }

    // ac: account-growth-chart — the cycle ends the app asks for, for a cycle that starts on the 26th
    @Test fun cycle_ends_are_the_day_before_each_cycle_start() {
        val ends = GetGrowthHistoryUseCase.cycleEnds(LocalDate.of(2026, 10, 15), startDay = 26)
        assertEquals(GetGrowthHistoryUseCase.MAX_CYCLES, ends.size)
        assertEquals(LocalDate.of(2026, 9, 25), ends.last())
        assertEquals(LocalDate.of(2026, 8, 25), ends[ends.size - 2])
        assertEquals(ends.sorted(), ends)
    }

    @Test fun with_a_cycle_that_starts_on_the_first_the_ends_are_month_ends() {
        val ends = GetGrowthHistoryUseCase.cycleEnds(LocalDate.of(2026, 10, 15), startDay = 1)
        assertEquals(LocalDate.of(2026, 9, 30), ends.last())
        assertEquals(LocalDate.of(2026, 8, 31), ends[ends.size - 2])
    }

    // ac: account-growth-toggle — with investment hidden the bands and the total leave it out
    @Test fun hiding_investment_leaves_it_out_of_the_total() {
        val h = history(
            3,
            acc("A", AccountType.CHECKING, 100.0, 150.0, 200.0),
            acc("I", AccountType.INVESTMENT, 50.0, 60.0, 70.0),
        )
        val s = GrowthSeriesBuilder.build(h, "USD", GrowthRange.ALL, includeInvestment = false)!!
        assertEquals(listOf(100.0, 150.0, 200.0), s.points.map { it.total })
        assertEquals(listOf(0.0, 0.0, 0.0), s.points.map { it.investment })
    }

    // The cut of the quiet run follows what is shown, so an investment that moved early does not
    // keep checking points that did not move.
    @Test fun the_quiet_run_is_cut_from_what_is_shown() {
        val h = history(
            4,
            acc("A", AccountType.CHECKING, 100.0, 100.0, 100.0, 130.0),
            acc("I", AccountType.INVESTMENT, 0.0, 10.0, 20.0, 30.0),
        )
        assertEquals(4, GrowthSeriesBuilder.build(h, "USD", GrowthRange.ALL)!!.points.size)
        assertEquals(2, GrowthSeriesBuilder.build(h, "USD", GrowthRange.ALL, includeInvestment = false)!!.points.size)
    }

    @Test fun a_currency_with_only_investment_has_nothing_to_draw_when_it_is_hidden() {
        val h = history(
            2,
            acc("A", AccountType.CHECKING, 1.0, 2.0),
            acc("I", AccountType.INVESTMENT, 1.0, 2.0, currency = "EUR"),
        )
        assertEquals(listOf("USD", "EUR"), GrowthSeriesBuilder.currencies(h))
        assertEquals(listOf("USD"), GrowthSeriesBuilder.currencies(h, includeInvestment = false))
        assertNull(GrowthSeriesBuilder.build(h, "EUR", GrowthRange.ALL, includeInvestment = false))
    }

    @Test fun the_switch_is_offered_only_when_there_is_an_investment_account() {
        assertTrue(GrowthSeriesBuilder.hasInvestment(history(2, acc("I", AccountType.INVESTMENT, 1.0, 2.0))))
        assertFalse(GrowthSeriesBuilder.hasInvestment(history(2, acc("A", AccountType.CHECKING, 1.0, 2.0))))
        assertFalse(GrowthSeriesBuilder.hasInvestment(history(2, acc("I", AccountType.INVESTMENT, 1.0, 2.0)), include = { false }))
    }
}
