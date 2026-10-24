package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.model.TransactionStatus
import com.fidriyanto.banktracker.domain.model.AmountRange
import com.fidriyanto.banktracker.domain.model.Rule
import com.fidriyanto.banktracker.domain.model.RuleAction
import com.fidriyanto.banktracker.domain.model.RuleConditions
import com.fidriyanto.banktracker.domain.model.TransactionUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleMinerTest {
    private var next = 1L
    private fun row(item: String, amount: Double, date: String = "2026-09-10", category: String = "Other", wallet: String = "BANK", type: String = "expense") =
        TransactionUiModel(next++, item, category, amount, date, wallet, type, TransactionStatus.SYNCED)

    private val generic = "Transfer from your account"

    @Test fun a_repeated_amount_on_one_account_is_suggested_with_the_category_the_labeled_rows_share() {
        val rows = List(5) { row(generic, 65.0, "2026-09-0${it + 1}") } + List(3) { row("BTS", 65.0, "2026-08-1${it}", "Transport") }
        val s = RuleMiner.suggest(rows, emptyList()).single { it.kind == SuggestionKind.AMOUNT }
        assertEquals(AmountRange(65.0, 65.0), s.conditions.amount)
        assertEquals(listOf("BANK"), s.conditions.accounts)
        assertEquals(5, s.rows)
        assertEquals("Transport", s.hintCategory)
        assertEquals(3, s.hintRows)
    }

    @Test fun an_amount_without_labeled_rows_has_no_category_hint() {
        val s = RuleMiner.suggest(List(4) { row(generic, 200.0, "2026-09-0${it + 1}") }, emptyList()).single()
        assertNull(s.hintCategory)
        assertEquals(SuggestionKind.AMOUNT, s.kind)
    }

    @Test fun a_payment_on_the_same_day_each_month_is_suggested_as_recurring() {
        val rows = listOf("2026-06-26", "2026-07-26", "2026-08-26", "2026-09-26").map { row(generic, 11500.0, it) }
        val s = RuleMiner.suggest(rows, emptyList()).single()
        assertEquals(SuggestionKind.RECURRING, s.kind)
        assertEquals(26, s.day)
        assertEquals(4, s.months)
        assertEquals(26, s.conditions.date?.day)
    }

    @Test fun an_amount_on_scattered_days_is_not_recurring() {
        val rows = listOf("2026-06-03", "2026-07-17", "2026-08-26", "2026-09-09").map { row(generic, 500.0, it) }
        assertEquals(SuggestionKind.AMOUNT, RuleMiner.suggest(rows, emptyList()).single().kind)
    }

    @Test fun a_text_is_suggested_only_when_one_amount_dominates_its_rows() {
        val steady = List(5) { row("Admin fee.", 2500.0, "2026-09-0${it + 1}") } + row("Admin fee.", 40000.0)
        assertEquals("admin fee", RuleMiner.suggest(steady, emptyList()).single { it.kind == SuggestionKind.TEXT }.conditions.text?.value)
        val vague = List(6) { row("Miscellaneous.", 1000.0 + it * 777, "2026-09-0${it + 1}") }
        assertTrue(RuleMiner.suggest(vague, emptyList()).none { it.kind == SuggestionKind.TEXT })
    }

    @Test fun an_amount_rule_inside_a_text_rule_is_dropped() {
        val rows = List(5) { row("Admin fee.", 2500.0, "2026-09-0${it + 1}") } + row("Admin fee.", 40000.0, "2026-09-20")
        val found = RuleMiner.suggest(rows, emptyList())
        assertEquals(listOf(SuggestionKind.TEXT), found.map { it.kind })
        assertEquals(6, found.single().rows)
    }

    @Test fun rows_that_already_have_a_category_or_are_transfers_are_ignored() {
        val rows = List(6) { row("Cafe", 50.0, "2026-09-0${it + 1}", category = "Food & Drink") } + List(6) { row("Move", 50.0, "2026-09-1$it", type = "transfer") }
        assertTrue(RuleMiner.suggest(rows, emptyList()).isEmpty())
    }

    @Test fun rows_that_an_existing_rule_sorts_are_left_out() {
        val rows = List(5) { row(generic, 65.0, "2026-09-0${it + 1}") }
        val rule = Rule("r", "r", true, RuleConditions(amount = AmountRange(65.0, 65.0)), RuleAction(category = "Transport"))
        assertTrue(RuleMiner.suggest(rows, listOf(rule)).isEmpty())
    }

    @Test fun too_few_repeats_give_no_suggestion_and_the_biggest_come_first() {
        assertTrue(RuleMiner.suggest(List(3) { row(generic, 29.0, "2026-09-0${it + 1}") }, emptyList()).isEmpty())
        val rows = List(4) { row(generic, 29.0, "2026-09-0${it + 1}") } + List(7) { row(generic, 200.0, "2026-09-1$it") } + List(10) { row(generic, 1000.0 + it, "2026-09-2$it") }
        assertEquals(listOf(7, 4), RuleMiner.suggest(rows, emptyList()).map { it.rows })
    }
}
