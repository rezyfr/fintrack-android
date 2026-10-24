package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.model.TransactionStatus
import com.fidriyanto.banktracker.domain.model.BudgetLine
import com.fidriyanto.banktracker.domain.model.TransactionUiModel
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class BudgetCompareBuilderTest {
    private val cycle = PayCycle.Range("2026-09-26", "2026-10-25", LocalDate.of(2026, 9, 26), LocalDate.of(2026, 10, 25))
    private val prev = PayCycle.Range("2026-08-26", "2026-09-25", LocalDate.of(2026, 8, 26), LocalDate.of(2026, 9, 25))

    private fun line(id: Long, name: String, currency: String) =
        BudgetLine(id, name, "flex", currency, 100.0, null, null, null, id.toInt(), name, null)

    private fun tx(id: Long, date: String, amount: Double) = TransactionUiModel(
        id = id, item = "x", category = "Food", amount = amount, dateIso = date, wallet = "BCA",
        txType = "expense", status = TransactionStatus.SYNCED,
    )

    private val thb = line(1, "Food", "THB")
    private val idr = line(2, "Food", "IDR")
    private val other = line(3, "Transport", "IDR")

    private val agg = mapOf(
        1L to BudgetAttribution.LineAgg(10.0, listOf(tx(1, "2026-10-05", 10.0))),
        2L to BudgetAttribution.LineAgg(30.0, listOf(tx(2, "2026-09-28", 30.0))),
        3L to BudgetAttribution.LineAgg(99.0, listOf(tx(3, "2026-09-29", 99.0))),
    )

    private fun compare(merge: Boolean) =
        buildBudgetCompare(cycle, prev, listOf(thb, idr, other), agg, emptyMap(), emptyList(), merge) { row, _ -> row.amount }

    @Test fun mergedChartAddsTheLinesWithTheSameName() {
        val byLine = compare(true).byLine
        assertEquals(40.0, byLine.getValue(1).points.last().cumulative, 0.0)
        assertEquals(40.0, byLine.getValue(2).points.last().cumulative, 0.0)
        assertEquals(99.0, byLine.getValue(3).points.last().cumulative, 0.0)
    }

    @Test fun unmergedChartKeepsEachLineApart() {
        val byLine = compare(false).byLine
        assertEquals(10.0, byLine.getValue(1).points.last().cumulative, 0.0)
        assertEquals(30.0, byLine.getValue(2).points.last().cumulative, 0.0)
    }
}
