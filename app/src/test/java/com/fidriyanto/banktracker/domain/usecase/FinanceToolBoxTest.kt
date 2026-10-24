package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.model.TransactionStatus
import com.fidriyanto.banktracker.data.repository.AccountRepository
import com.fidriyanto.banktracker.data.repository.BudgetLineRepository
import com.fidriyanto.banktracker.data.repository.CurrencySettingsRepository
import com.fidriyanto.banktracker.data.repository.TransactionRepository
import com.fidriyanto.banktracker.data.repository.WalletBalanceRepository
import com.fidriyanto.banktracker.domain.model.Account
import com.fidriyanto.banktracker.domain.model.CurrencySettings
import com.fidriyanto.banktracker.domain.model.TransactionUiModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FinanceToolBoxTest {
    private val rows = listOf(
        tx(1, "Coffee shop", "Food & Drink", 120.0, "BBL", "2026-10-06", "Coffee"),
        tx(2, "Noodles", "Food & Drink", 80.0, "BBL", "2026-10-07"),
        tx(3, "Rent", "Bills", 1200.0, "BBL", "2026-10-01"),
        tx(4, "Supermarket", "Groceries", 125000.0, "BCA", "2026-10-08"),
        tx(5, "Salary", "Income", 99999.0, "BBL", "2026-10-02", txType = "income"),
    )
    private val transactions = mockk<TransactionRepository>().also {
        coEvery { it.fetch(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns Result.success(rows)
        coEvery { it.recentTransferRates(any(), any()) } returns Result.success(emptyList())
    }
    private val accounts = mockk<AccountRepository>().also {
        every { it.observeAll() } returns flowOf(listOf(Account("BBL", "Bangkok Bank", "THB", true), Account("BCA", "BCA", "IDR", true)))
    }
    private val settings = mockk<CurrencySettingsRepository>().also { every { it.observe() } returns flowOf(CurrencySettings()) }
    private val box = FinanceToolBox(
        transactions, accounts, settings, mockk<BudgetLineRepository>(), mockk<WalletBalanceRepository>(), LocalDate.of(2026, 10, 11),
    )
    private val tools = box.tools().associateBy { it.name }

    private fun tx(id: Long, item: String, cat: String, amount: Double, wallet: String, date: String, sub: String? = null, txType: String = "expense") =
        TransactionUiModel(id, item, cat, amount, date, wallet, txType, TransactionStatus.SYNCED, subcategory = sub)

    private fun run(name: String, vararg args: Pair<String, String>) = runBlocking { tools.getValue(name).run(args.toMap()) }

    @Test fun total_for_one_category_counts_expenses_only() {
        val answer = run("spending_total", "category" to "Food & Drink", "period" to "this_month")
        assertTrue(answer, answer.contains("฿200"))
        assertTrue(answer, answer.contains("2 transactions"))
    }

    @Test fun a_part_of_the_category_name_is_enough() {
        assertTrue(run("spending_total", "category" to "Food", "period" to "this_month").contains("฿200"))
    }

    @Test fun total_matches_a_subcategory() {
        assertTrue(run("spending_total", "category" to "coffee").contains("฿120"))
    }

    @Test fun totals_stay_in_their_own_currency() {
        val answer = run("spending_total", "period" to "this_month")
        assertTrue(answer, answer.contains("฿1,400"))
        assertTrue(answer, answer.contains("Rp 125,000"))
    }

    @Test fun largest_expenses_ranks_across_currencies_and_honors_the_limit() {
        // 1,200 baht is about 642,000 rupiah at the default rate, so it is the largest. The supermarket comes second.
        val two = run("largest_expenses", "limit" to "2")
        assertTrue(two, two.contains("Rent ฿1,200"))
        assertTrue(two, two.contains("Supermarket Rp 125,000"))
        assertEquals(2, two.lines().count { it.contains(" on 2026-") })
        val one = run("largest_expenses", "limit" to "1")
        assertTrue(one, one.contains("Rent ฿1,200"))
        assertTrue(one, !one.contains("Supermarket"))
    }

    @Test fun a_number_in_the_question_wins_over_the_limit_the_model_passes() {
        val local = FinanceToolBox(
            transactions, accounts, settings, mockk<BudgetLineRepository>(), mockk<WalletBalanceRepository>(), LocalDate.of(2026, 10, 11), questionLimit = 1,
        ).tools().associateBy { it.name }
        val answer = runBlocking { local.getValue("largest_expenses").run(mapOf("limit" to "5")) }
        assertEquals(1, answer.lines().count { it.contains(" on 2026-") })
    }

    @Test fun a_tool_also_builds_a_table_with_exact_values() {
        run("largest_expenses", "limit" to "2")
        val table = box.tables.last()
        assertEquals("Largest expenses", table.title)
        assertEquals(listOf("Rent", "Supermarket"), table.sections.single().rows.map { it.label })
        assertEquals(listOf("฿1,200", "Rp 125,000"), table.sections.single().rows.map { it.value })
        assertEquals("1 Oct 2026", table.sections.single().rows.first().note)
        assertEquals("26 Sep to 11 Oct 2026", table.subtitle)
    }

    @Test fun compare_names_both_periods() {
        val answer = run("compare_spending", "period_a" to "this_week", "period_b" to "last_week")
        assertTrue(answer, answer.contains("from 2026-10-05 to 2026-10-11"))
        assertTrue(answer, answer.contains("from 2026-09-28 to 2026-10-04"))
    }

    @Test fun unknown_period_explains_the_choices() {
        assertTrue(run("spending_total", "period" to "someday").startsWith("Unknown period"))
    }

    @Test fun expense_filter_ignores_income() {
        assertEquals(4, SpendingQueries.expenses(rows, null).size)
    }

    @Test fun a_date_phrase_as_the_user_said_it_becomes_exact_dates() {
        val answer = run("spending_total", "category" to "Food", "period" to "first week of August 2026")
        assertTrue(answer, answer.contains("from 2026-08-01 to 2026-08-07"))
        assertTrue(answer, answer.contains("on Food"))
        io.mockk.coVerify { transactions.fetch(null, null, null, any(), any(), any(), any(), "2026-08-01", "2026-08-07", any()) }
    }

    @Test fun compare_accepts_month_names() {
        val answer = run("compare_spending", "period_a" to "August 2026", "period_b" to "September")
        assertTrue(answer, answer.contains("from 2026-08-01 to 2026-08-31"))
        assertTrue(answer, answer.contains("from 2026-09-01 to 2026-09-30"))
    }

    @Test fun a_phrase_that_cannot_be_read_names_the_ways_to_say_it() {
        val answer = run("spending_total", "period" to "sometime soon")
        assertTrue(answer, answer.startsWith("Unknown period"))
        assertTrue(answer, answer.contains("first week of August 2026"))
    }

    @Test fun income_total_sums_money_received_and_can_filter_by_name() {
        val all = run("income_total", "period" to "this_month")
        assertTrue(all, all.contains("฿99,999"))
        assertTrue(all, all.contains("1 transactions"))
        assertTrue(run("income_total", "text" to "salary", "period" to "this_month").contains("from salary"))
    }

    @Test fun cashflow_gives_money_in_out_and_net_per_currency() {
        val answer = run("cashflow", "period" to "this_month")
        assertTrue(answer, answer.contains("THB: in ฿99,999, out ฿1,400, net +฿98,599"))
        assertTrue(answer, answer.contains("IDR: in Rp 0, out Rp 125,000"))
    }

    @Test fun spending_stats_counts_totals_and_averages() {
        val answer = run("spending_stats", "category" to "Food", "period" to "this_month")
        assertTrue(answer, answer.contains("2 transactions, total ฿200, average ฿100, largest ฿120"))
    }

    @Test fun top_merchants_groups_by_name_and_honors_the_limit() {
        val two = run("top_merchants", "limit" to "2", "period" to "this_month")
        assertTrue(two, two.contains("Rent ฿1,200 (1 times)"))
        assertTrue(two, two.contains("Supermarket Rp 125,000 (1 times)"))
        assertTrue(run("top_merchants", "limit" to "1", "period" to "this_month").let { !it.contains("Supermarket") })
    }

    @Test fun find_transactions_can_filter_by_account_and_amount() {
        val answer = run("find_transactions", "account" to "bangkok", "min_amount" to "100")
        assertTrue(answer, answer.startsWith("Found"))
        io.mockk.coVerify { transactions.fetch(null, "BBL", null, any(), any(), 100.0, null, any(), any(), any()) }
        assertEquals("No account matches \"nowhere\".", run("find_transactions", "account" to "nowhere"))
    }

    @Test fun monthly_trend_lists_each_month_and_the_change_between_full_months() {
        val many = listOf(
            tx(10, "A", "Food & Drink", 100.0, "BBL", "2026-07-10"), tx(11, "B", "Food & Drink", 150.0, "BBL", "2026-08-10"),
            tx(12, "C", "Food & Drink", 120.0, "BBL", "2026-09-10"), tx(13, "D", "Food & Drink", 30.0, "BBL", "2026-10-02"),
        )
        io.mockk.coEvery { transactions.fetch(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns Result.success(many)
        val answer = run("monthly_trend", "category" to "Food", "months" to "4")
        assertTrue(answer, answer.contains("from 2026-07-01 to 2026-10-11"))
        assertTrue(answer, answer.contains("2026-07 ฿100, 2026-08 ฿150, 2026-09 ฿120, 2026-10 ฿30"))
        assertTrue(answer, answer.contains("Last full month against the one before: -20%"))
    }

    @Test fun recurring_payments_lists_what_repeats() {
        val many = listOf("2026-07-05", "2026-08-05", "2026-09-05").mapIndexed { i, d -> tx(20L + i, "Streaming", "Subscriptions", 12.0, "BBL", d) }
        io.mockk.coEvery { transactions.fetch(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns Result.success(many)
        val answer = run("recurring_payments")
        assertTrue(answer, answer.contains("Streaming ฿12, around day 5, 3 months, last on 2026-09-05"))
    }

    @Test fun a_category_breaks_down_by_subcategory() {
        val answer = run("spending_by_category", "category" to "Food", "period" to "this_month")
        val table = box.tables.last()
        assertEquals("Spending on Food by subcategory", table.title)
        assertEquals(listOf("Coffee", "No subcategory"), table.sections.single().rows.map { it.label })
        assertEquals(listOf("฿120", "฿80"), table.sections.single().rows.map { it.value })
        assertEquals("1 transaction", table.sections.single().rows.first().note)
        assertTrue(answer, answer.contains("THB: Coffee ฿120, No subcategory ฿80"))
    }

    @Test fun a_category_with_no_subcategories_breaks_down_by_name() {
        run("spending_by_category", "category" to "Bills", "period" to "this_month")
        val table = box.tables.last()
        assertEquals("Spending on Bills by name", table.title)
        assertEquals(listOf("Rent"), table.sections.single().rows.map { it.label })
    }

    @Test fun the_calls_of_a_question_are_recorded_with_their_arguments() {
        run("spending_by_category", "category" to "Food", "period" to "last week")
        assertEquals("spending_by_category(category=Food, period=last week)", box.calls.last())
    }

    @Test fun find_transactions_lists_the_transactions_of_a_category_with_their_dates() {
        run("find_transactions", "category" to "Food", "period" to "this_month")
        val table = box.tables.last()
        assertEquals("2 expenses on Food", table.title)
        assertEquals(listOf("Coffee shop", "Noodles"), table.sections.single().rows.map { it.label })
        assertEquals(listOf("฿120", "฿80"), table.sections.single().rows.map { it.value })
        assertEquals("6 Oct 2026", table.sections.single().rows.first().note)
    }

    @Test fun a_long_list_ends_with_how_many_more_there_are() {
        val many = (1..20).map { tx(100L + it, "Shop $it", "Groceries", 10.0 + it, "BBL", "2026-10-05") }
        io.mockk.coEvery { transactions.fetch(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns Result.success(many)
        run("find_transactions", "category" to "Groceries", "period" to "this_month")
        val rows = box.tables.last().sections.single().rows
        assertEquals(16, rows.size)
        assertEquals("and 5 more", rows.last().label)
    }
}
