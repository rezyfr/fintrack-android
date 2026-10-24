package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.model.TransactionStatus
import com.fidriyanto.banktracker.domain.model.BudgetLine
import com.fidriyanto.banktracker.domain.model.BudgetLineSpend
import com.fidriyanto.banktracker.domain.model.RateTable
import com.fidriyanto.banktracker.domain.model.TransactionUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InsightQueriesTest {
    private var next = 1L
    private fun row(item: String, amount: Double, date: String, type: String = "expense", wallet: String = "W") =
        TransactionUiModel(next++, item, "Other", amount, date, wallet, type, TransactionStatus.SYNCED)

    private val usd = { _: TransactionUiModel -> "USD" }

    @Test fun cash_flow_counts_income_and_expenses_and_ignores_transfers_and_investments() {
        val rows = listOf(row("Pay", 1000.0, "2026-10-01", "income"), row("Shop", 300.0, "2026-10-02"), row("Move", 500.0, "2026-10-03", "transfer"), row("Fund", 200.0, "2026-10-04", "investment"))
        val f = InsightQueries.flow(rows, usd).getValue("USD")
        assertEquals(1000.0, f.income, 0.0)
        assertEquals(300.0, f.expense, 0.0)
        assertEquals(700.0, f.net, 0.0)
    }

    @Test fun income_can_be_narrowed_to_a_name() {
        val rows = listOf(row("ACME salary", 1000.0, "2026-10-01", "income"), row("Gift", 50.0, "2026-10-02", "income"), row("Shop", 5.0, "2026-10-03"))
        assertEquals(2, InsightQueries.income(rows, null).size)
        assertEquals(listOf("ACME salary"), InsightQueries.income(rows, "acme").map { it.item })
    }

    @Test fun merchants_are_grouped_by_name_and_ranked_across_currencies_by_home_value() {
        val rows = listOf(
            row("Cafe", 10.0, "2026-10-01"), row("CAFE", 15.0, "2026-10-02"), row("Gym", 40.0, "2026-10-03"),
            row("Hotel", 20000.0, "2026-10-04", wallet = "JP"),
        )
        val cur = { r: TransactionUiModel -> if (r.wallet == "JP") "JPY" else "USD" }
        val rates = RateTable("USD", mapOf("JPY" to 0.007))
        val top = InsightQueries.merchants(rows, cur, rates, 3)
        // 20000 JPY is 140 USD, so it comes first.
        assertEquals(listOf("Hotel", "Gym", "Cafe"), top.map { it.name })
        assertEquals("JPY", top[0].currency)
        assertEquals(25.0, top[2].total, 0.0)
        assertEquals(2, top[2].times)
        assertEquals(1, InsightQueries.merchants(rows, cur, rates, 1).size)
    }

    @Test fun monthly_totals_are_listed_oldest_first() {
        val rows = listOf(row("A", 10.0, "2026-09-05"), row("B", 20.0, "2026-08-05"), row("C", 5.0, "2026-09-20"))
        val m = InsightQueries.monthly(rows, usd).getValue("USD")
        assertEquals(listOf("2026-08", "2026-09"), m.keys.toList())
        assertEquals(15.0, m.getValue("2026-09"), 0.0)
    }

    @Test fun a_named_payment_with_one_usual_amount_in_three_months_is_recurring() {
        val rows = listOf("2026-07-05", "2026-08-05", "2026-09-06", "2026-10-05").map { row("Streaming", 12.0, it) }
        val r = InsightQueries.recurring(rows, usd, 8).single()
        assertEquals("Streaming", r.name)
        assertEquals(12.0, r.amount, 0.0)
        assertEquals(5, r.day)
        assertEquals(4, r.months)
        assertEquals("2026-10-05", r.lastDate)
    }

    @Test fun the_same_amount_on_the_same_day_under_a_generic_name_is_recurring() {
        val generic = listOf("2026-07-26", "2026-08-26", "2026-09-26").map { row("Transfer from your account", 700.0, it) } +
            listOf("2026-07-02", "2026-08-19", "2026-09-30", "2026-10-07").mapIndexed { i, d -> row("Transfer from your account", 31.0 + i * 9, d) }
        val r = InsightQueries.recurring(generic, usd, 8)
        assertEquals(1, r.size)
        assertEquals(700.0, r.single().amount, 0.0)
        assertEquals(26, r.single().day)
    }

    @Test fun payments_in_fewer_than_three_months_are_not_recurring() {
        val rows = listOf("2026-09-05", "2026-10-05").map { row("Streaming", 12.0, it) }
        assertTrue(InsightQueries.recurring(rows, usd, 8).isEmpty())
    }

    @Test fun a_budget_line_is_found_by_name_or_category_in_either_direction() {
        fun spend(name: String, cat: String?) = BudgetLineSpend(BudgetLine(1, name, "flex", "USD", 100.0, null, null, null, 0, category = cat), 40.0)
        val lines = listOf(spend("Eating out", "Food & Drink"), spend("Rent", null))
        assertEquals("Eating out", InsightQueries.budgetLine(lines, "food")?.line?.name)
        assertEquals("Rent", InsightQueries.budgetLine(lines, "the rent line")?.line?.name)
        assertNull(InsightQueries.budgetLine(lines, "travel"))
    }
}
