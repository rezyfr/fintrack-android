package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.domain.model.RateTable
import com.fidriyanto.banktracker.data.model.TransactionStatus
import com.fidriyanto.banktracker.domain.model.TransactionUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DashboardSummaryBuilderTest {
    private val flow = MoneyFlow(0.0, 0.0)

    private fun tx(id: Long, amount: Double, category: String = "Food", wallet: String = "BCA", date: String = "2026-10-01", type: String = "expense") =
        TransactionUiModel(
            id = id, item = "item$id", category = category, amount = amount, dateIso = date, wallet = wallet,
            txType = type, status = TransactionStatus.SYNCED,
        )

    @Test fun categoryRowsCarryPreviousSpendAndZeroForNewCategories() {
        val current = listOf(tx(1, 100.0, "Food"), tx(2, 50.0, "Groceries"))
        val previous = listOf(tx(3, 80.0, "Food"), tx(4, 999.0, "Food", type = "income"))
        val rows = buildSummaryFromTransactions(current, flow, previous).categoryBreakdown.associateBy { it.category }
        assertEquals(80.0, rows.getValue("Food").previousAmount!!, 0.0)
        assertEquals(0.0, rows.getValue("Groceries").previousAmount!!, 0.0)
    }

    @Test fun categoryRowsHaveNoPreviousSpendWhenPreviousWasNotLoaded() {
        val rows = buildSummaryFromTransactions(listOf(tx(1, 100.0)), flow).categoryBreakdown
        assertNull(rows.single().previousAmount)
    }

    @Test fun topExpensesAreTheLargestFiveExpensesOnly() {
        val current = (1L..7L).map { tx(it, it * 10.0) } + tx(8, 1000.0, type = "income")
        val top = buildSummaryFromTransactions(current, flow).topExpenses
        assertEquals(listOf(70.0, 60.0, 50.0, 40.0, 30.0), top.map { it.amount })
    }

    @Test fun walletBreakdownSplitsExpensesByWalletLargestFirst() {
        val current = listOf(tx(1, 30.0, wallet = "BCA"), tx(2, 10.0, wallet = "MANDIRI"), tx(3, 60.0, wallet = "BCA"))
        val wallets = buildSummaryFromTransactions(current, flow).walletBreakdown
        assertEquals(listOf("BCA", "MANDIRI"), wallets.map { it.wallet })
        assertEquals(0.9f, wallets.first().percentage, 0.001f)
    }

    @Test fun dailySpendSumsExpensesPerDate() {
        val current = listOf(tx(1, 30.0, date = "2026-10-01"), tx(2, 20.0, date = "2026-10-01T09:00"), tx(3, 5.0, date = "2026-10-02"))
        val daily = buildSummaryFromTransactions(current, flow).dailySpend
        assertEquals(mapOf("2026-10-01" to 50.0, "2026-10-02" to 5.0), daily)
    }

    @Test fun monthlyTrendHasOnePointPerCycleAndMarksTheCurrentOne() {
        val today = LocalDate.of(2026, 10, 9)
        val cycles = listOf(PayCycle.cycleRange(1, today), PayCycle.cycleRange(0, today))
        val rows = listOf(
            listOf(tx(1, 40.0, date = "2026-09-01"), tx(2, 100.0, date = "2026-09-01", type = "income")),
            listOf(tx(3, 20.0, date = "2026-10-01")),
        )
        val trend = buildMonthlyTrend(cycles, rows, "IDR", { "IDR" }, today)
        assertEquals(listOf(100.0, 0.0), trend.map { it.moneyIn })
        assertEquals(listOf(40.0, 20.0), trend.map { it.moneyOut })
        assertTrue(trend.last().inProgress)
        assertEquals("2026-10-25", trend.last().endIso)
    }

    @Test fun convertToHomeMultipliesForeignRowsAndForeignReceivedAmounts() {
        val currencyOf: (String?) -> String = { if (it == "BBL") "THB" else "IDR" }
        val rows = listOf(
            tx(1, 10.0, wallet = "BBL"),
            tx(2, 500.0, wallet = "BCA"),
            tx(3, 5350.0, wallet = "BCA", type = "transfer").copy(toWallet = "BBL", toAmount = 10.0),
        )
        val out = convertToHome(rows, RateTable("IDR", mapOf("THB" to 535.0)), currencyOf)
        assertEquals(listOf(5350.0, 500.0, 5350.0), out.map { it.amount })
        assertEquals(5350.0, out[2].toAmount!!, 0.0)
    }

    @Test fun aSingleSubcategoryIsStillShown() {
        val rows = listOf(
            tx(1, 40.0, "Transport").copy(subcategory = "Public Transport"),
            tx(2, 30.0, "Transport").copy(subcategory = "Public Transport"),
        )
        val subs = buildSummaryFromTransactions(rows, flow).subcategoryBreakdown.getValue("Transport")
        assertEquals(listOf("Public Transport"), subs.map { it.category })
    }

    @Test fun aCategoryWithNoSubcategoriesHasNoBreakdown() {
        val rows = listOf(tx(1, 40.0, "Groceries"), tx(2, 30.0, "Groceries"))
        assertTrue(buildSummaryFromTransactions(rows, flow).subcategoryBreakdown.isEmpty())
    }
}
