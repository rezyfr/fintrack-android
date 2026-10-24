package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.domain.model.AmountRange
import com.fidriyanto.banktracker.domain.model.DateMatch
import com.fidriyanto.banktracker.domain.model.Rule
import com.fidriyanto.banktracker.domain.model.RuleAction
import com.fidriyanto.banktracker.domain.model.RuleConditions
import com.fidriyanto.banktracker.domain.model.RuleInput
import com.fidriyanto.banktracker.domain.model.TextMatch
import com.fidriyanto.banktracker.domain.model.TextMode
import com.fidriyanto.banktracker.domain.model.TimeRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class RuleEngineTest {
    private val noon = LocalDateTime.of(2026, 12, 25, 13, 0)

    private fun input(text: String = "Grab TH", amount: Double = 55.0, at: LocalDateTime = noon, type: String = "expense", account: String? = "MAIN") =
        RuleInput(text, amount, type, account, at)

    private fun rule(id: String, c: RuleConditions, a: RuleAction, enabled: Boolean = true) = Rule(id, id, enabled, c, a)

    private val contains = RuleConditions(text = TextMatch(TextMode.CONTAINS, "grab"))

    @Test fun text_matches_without_regard_to_case_and_by_mode() {
        val r = { m: TextMode, v: String -> rule("r", RuleConditions(text = TextMatch(m, v)), RuleAction(category = "X")) }
        assertTrue(RuleEngine.matches(r(TextMode.CONTAINS, "GRAB"), input()))
        assertTrue(RuleEngine.matches(r(TextMode.STARTS_WITH, "grab"), input()))
        assertFalse(RuleEngine.matches(r(TextMode.STARTS_WITH, "th"), input()))
        assertTrue(RuleEngine.matches(r(TextMode.EQUALS, "grab th"), input()))
        assertFalse(RuleEngine.matches(r(TextMode.EQUALS, "grab"), input()))
    }

    @Test fun amount_range_includes_both_bounds_and_allows_one_open_side() {
        val r = { min: Double?, max: Double? -> rule("r", RuleConditions(amount = AmountRange(min, max)), RuleAction(category = "X")) }
        assertTrue(RuleEngine.matches(r(55.0, 55.0), input()))
        assertTrue(RuleEngine.matches(r(null, 60.0), input()))
        assertTrue(RuleEngine.matches(r(50.0, null), input()))
        assertFalse(RuleEngine.matches(r(56.0, null), input()))
    }

    @Test fun time_range_works_and_may_cross_midnight() {
        val r = { f: Int, t: Int -> rule("r", RuleConditions(time = TimeRange(f, t)), RuleAction(category = "X")) }
        assertTrue(RuleEngine.matches(r(12 * 60 + 30, 13 * 60 + 30), input()))
        assertFalse(RuleEngine.matches(r(14 * 60, 15 * 60), input()))
        val lateNight = input(at = LocalDateTime.of(2026, 12, 25, 1, 0))
        assertTrue(RuleEngine.matches(r(22 * 60, 2 * 60), lateNight))
        assertFalse(RuleEngine.matches(r(22 * 60, 2 * 60), input()))
    }

    @Test fun date_matches_day_and_month_every_year_and_weekdays() {
        val r = { d: DateMatch -> rule("r", RuleConditions(date = d), RuleAction(category = "X")) }
        assertTrue(RuleEngine.matches(r(DateMatch(day = 25, month = 12)), input()))
        assertFalse(RuleEngine.matches(r(DateMatch(day = 25, month = 11)), input()))
        assertTrue(RuleEngine.matches(r(DateMatch(year = 2026)), input()))
        // 25 Dec 2026 is a Friday.
        assertTrue(RuleEngine.matches(r(DateMatch(weekdays = listOf(5, 6))), input()))
        assertFalse(RuleEngine.matches(r(DateMatch(weekdays = listOf(1))), input()))
    }

    @Test fun a_range_of_days_includes_both_ends_and_may_cross_the_end_of_the_month() {
        val r = { from: Int, to: Int -> rule("r", RuleConditions(date = DateMatch(day = from, dayTo = to)), RuleAction(category = "X")) }
        // The input date is the 25th.
        assertTrue(RuleEngine.matches(r(25, 27), input()))
        assertTrue(RuleEngine.matches(r(20, 25), input()))
        assertFalse(RuleEngine.matches(r(2, 5), input()))
        assertTrue(RuleEngine.matches(r(2, 5), input(at = LocalDateTime.of(2026, 12, 3, 9, 0))))
        assertTrue(RuleEngine.matches(r(24, 3), input()))
        assertTrue(RuleEngine.matches(r(24, 3), input(at = LocalDateTime.of(2026, 12, 2, 9, 0))))
        assertFalse(RuleEngine.matches(r(24, 3), input(at = LocalDateTime.of(2026, 12, 10, 9, 0))))
    }

    @Test fun a_rule_with_no_condition_matches_nothing() {
        assertFalse(RuleEngine.matches(rule("r", RuleConditions(), RuleAction(category = "X")), input()))
        assertTrue(RuleEngine.matches(rule("r", RuleConditions(amount = AmountRange(1.0, 100.0)), RuleAction(category = "X")), input(text = "")))
    }

    @Test fun day_ranges_that_overlap_make_a_conflict() {
        val a = rule("a", RuleConditions(date = DateMatch(day = 2, dayTo = 5)), RuleAction(category = "A"))
        val b = rule("b", RuleConditions(date = DateMatch(day = 4, dayTo = 8)), RuleAction(category = "B"))
        val c = rule("c", RuleConditions(date = DateMatch(day = 10, dayTo = 12)), RuleAction(category = "C"))
        assertEquals(1, RuleConflicts.find(listOf(a, b)).size)
        assertTrue(RuleConflicts.find(listOf(a, c)).isEmpty())
    }

    @Test fun account_and_type_limit_a_rule() {
        val r = rule("r", RuleConditions(accounts = listOf("MAIN"), type = "expense"), RuleAction(category = "X"))
        assertTrue(RuleEngine.matches(r, input()))
        assertFalse(RuleEngine.matches(r, input(account = "OTHER")))
        assertFalse(RuleEngine.matches(r, input(type = "income")))
    }

    @Test fun a_disabled_rule_never_matches() {
        assertFalse(RuleEngine.matches(rule("r", contains, RuleAction(category = "X"), enabled = false), input()))
    }

    @Test fun detail_counts_the_conditions() {
        assertEquals(0, RuleEngine.detail(RuleConditions()))
        assertEquals(3, RuleEngine.detail(RuleConditions(text = TextMatch(TextMode.CONTAINS, "a"), amount = AmountRange(1.0, null), accounts = listOf("A", "B"))))
    }

    @Test fun the_most_detailed_rule_wins_and_a_less_detailed_rule_fills_the_gaps() {
        val general = rule("general", contains, RuleAction(category = "Transport", subcategory = "Taxi", description = "Ride"))
        val detailed = rule("detailed", contains.copy(amount = AmountRange(20.0, 80.0)), RuleAction(category = "Transport", description = "Late taxi"))
        val out = RuleEngine.apply(listOf(general, detailed), input())
        assertEquals("Transport", out.category)
        assertEquals("Taxi", out.subcategory)
        assertEquals("Late taxi", out.description)
    }

    @Test fun a_tie_goes_to_the_rule_higher_in_the_list() {
        val a = rule("a", contains, RuleAction(category = "First"))
        val b = rule("b", contains, RuleAction(category = "Second"))
        assertEquals("First", RuleEngine.apply(listOf(a, b), input()).category)
        assertEquals("Second", RuleEngine.apply(listOf(b, a), input()).category)
    }

    @Test fun a_subcategory_is_taken_only_from_a_rule_with_the_chosen_category() {
        val general = rule("general", contains, RuleAction(category = "Transport", subcategory = "Taxi"))
        val detailed = rule("detailed", contains.copy(amount = AmountRange(1.0, 100.0)), RuleAction(category = "Shopping"))
        val out = RuleEngine.apply(listOf(general, detailed), input())
        assertEquals("Shopping", out.category)
        assertNull(out.subcategory)
    }

    @Test fun the_description_fills_merchant_and_amount() {
        val r = rule("r", contains, RuleAction(category = "Transport", description = "{merchant} for {amount}"))
        assertEquals("Grab TH for 55", RuleEngine.apply(listOf(r), input()).description)
        assertEquals("Grab TH for 55.50", RuleEngine.apply(listOf(r), input(amount = 55.5)).description)
    }

    @Test fun an_ignore_rule_ignores_the_transaction_unless_a_more_detailed_rule_sets_a_category() {
        val ignore = rule("ignore", contains, RuleAction(ignore = true))
        assertTrue(RuleEngine.apply(listOf(ignore), input()).ignore)
        val keep = rule("keep", contains.copy(amount = AmountRange(1.0, 100.0)), RuleAction(category = "Transport"))
        assertFalse(RuleEngine.apply(listOf(ignore, keep), input()).ignore)
    }

    @Test fun no_matching_rule_gives_an_empty_outcome() {
        assertTrue(RuleEngine.apply(listOf(rule("r", RuleConditions(text = TextMatch(TextMode.CONTAINS, "zzz")), RuleAction(category = "X"))), input()).isEmpty)
    }

    @Test fun two_rules_with_the_same_detail_and_different_results_conflict() {
        val a = rule("a", contains, RuleAction(category = "Transport"))
        val b = rule("b", RuleConditions(text = TextMatch(TextMode.CONTAINS, "gra")), RuleAction(category = "Shopping"))
        assertEquals(1, RuleConflicts.find(listOf(a, b)).size)
    }

    @Test fun rules_that_cannot_overlap_or_agree_do_not_conflict() {
        val a = rule("a", contains.copy(amount = AmountRange(1.0, 10.0)), RuleAction(category = "Transport"))
        val b = rule("b", contains.copy(amount = AmountRange(20.0, 30.0)), RuleAction(category = "Shopping"))
        assertTrue(RuleConflicts.find(listOf(a, b)).isEmpty())
        val c = rule("c", contains, RuleAction(category = "Transport"))
        assertTrue(RuleConflicts.find(listOf(c, c.copy(id = "d"))).isEmpty())
        val e = rule("e", contains.copy(amount = AmountRange(1.0, 100.0)), RuleAction(category = "Shopping"))
        assertTrue(RuleConflicts.find(listOf(c, e)).isEmpty())
    }
}
