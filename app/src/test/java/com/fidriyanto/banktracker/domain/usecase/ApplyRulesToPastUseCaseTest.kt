package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.model.TransactionEdit
import com.fidriyanto.banktracker.data.model.TransactionStatus
import com.fidriyanto.banktracker.data.repository.TransactionRepository
import com.fidriyanto.banktracker.domain.model.AmountRange
import com.fidriyanto.banktracker.domain.model.Rule
import com.fidriyanto.banktracker.domain.model.RuleAction
import com.fidriyanto.banktracker.domain.model.RuleConditions
import com.fidriyanto.banktracker.domain.model.TextMatch
import com.fidriyanto.banktracker.domain.model.TextMode
import com.fidriyanto.banktracker.domain.model.TimeRange
import com.fidriyanto.banktracker.domain.model.TransactionUiModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ApplyRulesToPastUseCaseTest {
    private val today = LocalDate.of(2026, 10, 12)

    private fun row(id: Long, item: String, amount: Double = 55.0, category: String = "Other", type: String = "expense", sub: String? = null) =
        TransactionUiModel(id, item, category, amount, "2026-10-10", "MAIN", type, TransactionStatus.SYNCED, subcategory = sub)

    private fun rule(action: RuleAction, c: RuleConditions = RuleConditions(text = TextMatch(TextMode.CONTAINS, "grab"))) = Rule("r", "r", true, c, action)

    private fun repo(vararg rows: TransactionUiModel) = mockk<TransactionRepository>().also {
        coEvery { it.fetch(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns Result.success(rows.toList())
    }

    @Test fun preview_lists_only_rows_that_would_change() = runBlocking {
        val useCase = ApplyRulesToPastUseCase(repo(row(1, "GRAB TH"), row(2, "Cafe"), row(3, "grab bike", category = "Transport")))
        val changes = useCase.preview(rule(RuleAction(category = "Transport")), 30, today).getOrThrow()
        assertEquals(listOf(1L), changes.map { it.row.id })
        assertEquals("Transport", changes.single().category)
    }

    @Test fun preview_asks_for_the_last_days_only() = runBlocking {
        val repo = repo()
        ApplyRulesToPastUseCase(repo).preview(rule(RuleAction(category = "Transport")), 30, today)
        coVerify { repo.fetch(null, null, null, any(), any(), any(), any(), "2026-09-12", "2026-10-12", any()) }
    }

    @Test fun a_rule_description_and_subcategory_are_part_of_the_change() = runBlocking {
        val useCase = ApplyRulesToPastUseCase(repo(row(1, "GRAB TH", sub = "Old")))
        val c = useCase.preview(rule(RuleAction(category = "Transport", subcategory = "Taxi", description = "{merchant} ride")), 30, today).getOrThrow().single()
        assertEquals("Taxi", c.subcategory)
        assertEquals("GRAB TH ride", c.item)
    }

    @Test fun transfers_stay_out_unless_the_rule_asks_for_them() = runBlocking {
        val rows = repo(row(1, "GRAB", type = "transfer"))
        assertTrue(ApplyRulesToPastUseCase(rows).preview(rule(RuleAction(category = "Transport")), 30, today).getOrThrow().isEmpty())
        val c = RuleConditions(text = TextMatch(TextMode.CONTAINS, "grab"), type = "transfer")
        assertEquals(1, ApplyRulesToPastUseCase(rows).preview(rule(RuleAction(category = "Transport"), c), 30, today).getOrThrow().size)
    }

    @Test fun a_rule_with_a_time_or_an_ignore_action_is_not_tried_on_saved_rows() = runBlocking {
        val useCase = ApplyRulesToPastUseCase(repo(row(1, "GRAB")))
        val timed = rule(RuleAction(category = "Transport"), RuleConditions(time = TimeRange(0, 600)))
        assertEquals(PastSupport.HAS_TIME, useCase.support(timed))
        assertTrue(useCase.preview(timed, 30, today).getOrThrow().isEmpty())
        assertEquals(PastSupport.IGNORE, useCase.support(rule(RuleAction(ignore = true))))
    }

    @Test fun the_amount_condition_is_checked() = runBlocking {
        val c = RuleConditions(text = TextMatch(TextMode.CONTAINS, "grab"), amount = AmountRange(100.0, null))
        assertTrue(ApplyRulesToPastUseCase(repo(row(1, "GRAB", amount = 55.0))).preview(rule(RuleAction(category = "Transport"), c), 30, today).getOrThrow().isEmpty())
    }

    @Test fun apply_edits_each_row_and_counts_failures_and_undo_restores_the_old_values() = runBlocking {
        val rows = listOf(row(1, "GRAB A"), row(2, "GRAB B"))
        val repo = repo(*rows.toTypedArray())
        val edit = slot<TransactionEdit>()
        coEvery { repo.editTransaction(1, capture(edit)) } returns Result.success(Unit)
        coEvery { repo.editTransaction(2, any()) } returns Result.failure(Exception("x"))
        val useCase = ApplyRulesToPastUseCase(repo)
        val changes = useCase.preview(rule(RuleAction(category = "Transport")), 30, today).getOrThrow()
        val progress = mutableListOf<Int>()
        val result = useCase.apply(changes) { progress += it }
        assertEquals(ApplyResult(1, 1), result)
        assertEquals(listOf(1, 2), progress)
        assertEquals("Transport", edit.captured.category)
        useCase.undo(changes.take(1))
        assertEquals("Other", edit.captured.category)
    }
}
