package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.model.ParsedTransaction
import com.fidriyanto.banktracker.data.repository.RuleRepository
import com.fidriyanto.banktracker.domain.model.Rule
import com.fidriyanto.banktracker.domain.model.RuleAction
import com.fidriyanto.banktracker.domain.model.RuleConditions
import com.fidriyanto.banktracker.domain.model.TextMatch
import com.fidriyanto.banktracker.domain.model.TextMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

private class FakeRuleRepository(initial: List<Rule> = emptyList()) : RuleRepository {
    val state = MutableStateFlow(initial)
    override fun observe(): Flow<List<Rule>> = state
    override suspend fun replaceAll(rules: List<Rule>) { state.value = rules }
    override suspend fun refresh(): Result<Unit> = Result.success(Unit)
}

class RuleUseCasesTest {
    private fun rule(id: String, text: String, action: RuleAction) =
        Rule(id, id, true, RuleConditions(text = TextMatch(TextMode.CONTAINS, text)), action)

    @Test fun save_adds_a_new_rule_at_the_end_and_keeps_the_place_of_an_old_one() = runBlocking {
        val repo = FakeRuleRepository()
        val manage = ManageRulesUseCase(repo)
        manage.save(rule("a", "x", RuleAction(category = "A")))
        manage.save(rule("b", "y", RuleAction(category = "B")))
        manage.save(rule("a", "x", RuleAction(category = "A2")))
        assertEquals(listOf("a", "b"), repo.state.value.map { it.id })
        assertEquals("A2", repo.state.value.first().action.category)
    }

    @Test fun move_changes_the_order_and_stops_at_the_ends() = runBlocking {
        val repo = FakeRuleRepository(listOf(rule("a", "x", RuleAction()), rule("b", "y", RuleAction()), rule("c", "z", RuleAction())))
        val manage = ManageRulesUseCase(repo)
        manage.move("c", -1)
        assertEquals(listOf("a", "c", "b"), repo.state.value.map { it.id })
        manage.move("a", -1)
        assertEquals(listOf("a", "c", "b"), repo.state.value.map { it.id })
        manage.delete("c")
        assertEquals(listOf("a", "b"), repo.state.value.map { it.id })
        assertNull(manage.find("c"))
    }

    private val zone = ZoneId.of("Asia/Bangkok")
    private fun parsed(item: String) = ParsedTransaction(
        item = item, amount = 55.0, date = LocalDate.of(2026, 12, 25), referenceNo = "",
        timestampMs = LocalDate.of(2026, 12, 25).atTime(13, 0).atZone(zone).toInstant().toEpochMilli(), wallet = "MAIN",
    )

    @Test fun capture_takes_the_category_and_description_from_a_rule() = runBlocking {
        val apply = ApplyRulesUseCase(FakeRuleRepository(listOf(rule("r", "grab", RuleAction(category = "Transport", subcategory = "Taxi", description = "Ride")))))
        val out = apply.forCapture(parsed("GRAB TH"), zone)!!
        assertEquals("Transport", out.category)
        assertEquals("Taxi", out.subcategory)
        assertEquals("Ride", out.item)
    }

    @Test fun capture_keeps_the_parsed_values_when_no_rule_matches() = runBlocking {
        val apply = ApplyRulesUseCase(FakeRuleRepository(listOf(rule("r", "grab", RuleAction(category = "Transport")))))
        val out = apply.forCapture(parsed("Cafe"), zone)!!
        assertEquals("Other", out.category)
        assertEquals("Cafe", out.item)
    }

    @Test fun capture_returns_null_when_a_rule_says_ignore() = runBlocking {
        val apply = ApplyRulesUseCase(FakeRuleRepository(listOf(rule("r", "promo", RuleAction(ignore = true)))))
        assertNull(apply.forCapture(parsed("Promo offer"), zone))
    }
}
