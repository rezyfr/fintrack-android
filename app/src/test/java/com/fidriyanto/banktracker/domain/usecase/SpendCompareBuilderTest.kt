package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.model.TransactionStatus
import com.fidriyanto.banktracker.domain.model.TransactionUiModel
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class SpendCompareBuilderTest {
    private fun range(start: LocalDate, end: LocalDate) = PayCycle.Range(start.toString(), end.toString(), start, end)

    private fun tx(id: Long, date: String, amount: Double) = TransactionUiModel(
        id = id, item = "x", category = "c", amount = amount, dateIso = date, wallet = "BCA",
        txType = "expense", status = TransactionStatus.SYNCED,
    )

    private val cycle = range(LocalDate.of(2026, 9, 26), LocalDate.of(2026, 10, 25))
    private val prev = range(LocalDate.of(2026, 8, 26), LocalDate.of(2026, 9, 25))

    @Test fun lastCycleIsCumulativeAcrossItsWholeLength() {
        val prevRows = listOf(tx(1, "2026-08-27", 100.0), tx(2, "2026-08-27", 50.0), tx(3, "2026-09-10", 25.0))
        val sc = buildSpendCompare(cycle, prev, emptyList(), prevRows) { it.amount }
        assertEquals(31, sc.lastPoints.size)
        assertEquals(0.0, sc.lastPoints[0].cumulative, 0.0)
        assertEquals(150.0, sc.lastPoints[1].cumulative, 0.0)
        assertEquals(175.0, sc.lastPoints.last().cumulative, 0.0)
    }

    @Test fun rowsWithZeroSpendAreIgnored() {
        val prevRows = listOf(tx(1, "2026-08-27", 100.0), tx(2, "2026-08-28", 40.0))
        val sc = buildSpendCompare(cycle, prev, emptyList(), prevRows) { if (it.id == 1L) it.amount else 0.0 }
        assertEquals(100.0, sc.lastPoints.last().cumulative, 0.0)
    }

    private val two = range(LocalDate.of(2026, 7, 26), LocalDate.of(2026, 8, 25))
    private val three = range(LocalDate.of(2026, 6, 26), LocalDate.of(2026, 7, 25))

    @Test fun olderCyclesGetTheirOwnCurves() {
        val older = listOf(two to listOf(tx(1, "2026-07-27", 60.0)), three to listOf(tx(2, "2026-06-27", 90.0)))
        val sc = buildSpendCompare(cycle, prev, emptyList(), emptyList(), older) { it.amount }
        assertEquals(2, sc.older.size)
        assertEquals(60.0, sc.older[0].last().cumulative, 0.0)
        assertEquals(90.0, sc.older[1].last().cumulative, 0.0)
    }

    @Test fun aQuietCycleBetweenTwoBusyOnesStays() {
        val older = listOf(two to emptyList<TransactionUiModel>(), three to listOf(tx(2, "2026-06-27", 90.0)))
        val sc = buildSpendCompare(cycle, prev, emptyList(), emptyList(), older) { it.amount }
        assertEquals(2, sc.older.size)
        assertEquals(0.0, sc.older[0].last().cumulative, 0.0)
    }

    @Test fun emptyCyclesAtTheFarEndAreDropped() {
        val older = listOf(two to listOf(tx(1, "2026-07-27", 60.0)), three to emptyList<TransactionUiModel>())
        val sc = buildSpendCompare(cycle, prev, emptyList(), emptyList(), older) { it.amount }
        assertEquals(1, sc.older.size)
    }
}
