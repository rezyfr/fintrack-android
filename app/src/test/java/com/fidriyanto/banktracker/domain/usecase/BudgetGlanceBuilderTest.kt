package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.domain.model.RateTable
import com.fidriyanto.banktracker.data.model.TransactionStatus
import com.fidriyanto.banktracker.domain.model.BudgetLine
import com.fidriyanto.banktracker.domain.model.TransactionUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class BudgetGlanceBuilderTest {
    private val today = LocalDate.of(2026, 10, 10)
    private val cycle = PayCycle.cycleRange(0, today)
    private val wallets = mapOf("BBL" to "THB", "BCA" to "IDR")

    private fun line(id: Long, name: String, target: Double, currency: String = "THB", kind: String = "flex", pattern: String) =
        BudgetLine(id, name, kind, currency, target, listOf("BBL", "BCA"), pattern, null, id.toInt())

    private fun expense(id: Long, item: String, amount: Double, wallet: String = "BBL") = TransactionUiModel(
        id = id, item = item, category = "Other", amount = amount, dateIso = "2026-10-01", wallet = wallet,
        txType = "expense", status = TransactionStatus.SYNCED,
    )

    private val lines = listOf(
        line(1, "Groceries", 5000.0, pattern = "MARKET"),
        line(2, "Shopping", 0.0, pattern = "WEBSHOP"),
        line(3, "Food", 2000000.0, currency = "IDR", pattern = "TAKEOUT"),
    )
    private val rows = listOf(
        expense(10, "MARKET", 2100.0),
        expense(11, "WEBSHOP", 60.0),
        expense(12, "Repair Service", 1600.0),
        expense(13, "TAKEOUT", 500000.0, wallet = "BCA"),
    )

    private fun glance(currency: String?, balances: List<Pair<String, Double>>? = null, current: Boolean = true) = buildBudgetGlance(
        BudgetGlanceInput(
            currency = currency, rates = RateTable("IDR", mapOf("THB" to 500.0)),
            cycle = cycle, prevCycle = PayCycle.cycleRange(1, today), isCurrentCycle = current,
            lines = lines, currencyByWallet = wallets, rows = rows, prevRows = emptyList(),
            balances = balances, today = today,
        )
    )

    @Test fun unbudgetedSpendIsShownAndCounted() {
        val g = glance("THB")
        assertEquals(1600.0, g.unbudgetedSpent, 0.0)
        assertEquals(listOf("Repair Service"), g.unbudgetedRecent.map { it.item })
        assertEquals(2100.0 + 60.0 + 1600.0, g.totalSpent, 0.0)
    }

    @Test fun overspendReducesWhatIsLeft() {
        // 5,000 target minus 2,100 groceries, 60 over the 0 shopping target, and 1,600 unbudgeted
        assertEquals(5000.0 - 2100.0 - 60.0 - 1600.0, glance("THB").remaining, 0.001)
    }

    @Test fun dailyAllowanceIsCappedAtTheBalance() {
        val g = glance("THB", balances = listOf("THB" to 1000.0, "IDR" to 9000000.0))
        assertEquals(1000.0, g.balance!!, 0.0)
        assertEquals(1000.0 / g.daysLeft, g.dailyAllowance, 0.001)
    }

    @Test fun dailyAllowanceUsesWhatIsLeftWhenTheBalanceIsLarger() {
        val g = glance("THB", balances = listOf("THB" to 50000.0))
        assertEquals(g.remaining / g.daysLeft, g.dailyAllowance, 0.001)
    }

    @Test fun combinedViewConvertsAtTheRate() {
        val g = glance(null, balances = listOf("THB" to 1000.0, "IDR" to 200000.0))
        assertEquals(5000.0 * 500 + 2000000.0, g.totalTarget, 0.001)
        assertEquals(1600.0 * 500, g.unbudgetedSpent, 0.001)
        assertEquals(1000.0 * 500 + 200000.0, g.balance!!, 0.001)
    }

    @Test fun pastCycleHasNoBalance() = assertNull(glance("THB", balances = null, current = false).balance)
}
