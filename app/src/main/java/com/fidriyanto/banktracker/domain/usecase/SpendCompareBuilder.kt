package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.domain.model.SpendCompare
import com.fidriyanto.banktracker.domain.model.SpendPoint
import com.fidriyanto.banktracker.domain.model.TransactionUiModel
import java.time.LocalDate

// ac: home-cycle-spending-comparison — cumulative combined spend per day for both cycles. This cycle
// runs day 0..today; the previous cycle runs its whole length. Both are indexed by day-of-cycle so they
// share one x scale (maxDay = the longer cycle's last day index).
internal fun buildSpendCompare(
    cycle: PayCycle.Range,
    prevCycle: PayCycle.Range,
    rows: List<TransactionUiModel>,
    prevRows: List<TransactionUiModel>,
    olderCycles: List<Pair<PayCycle.Range, List<TransactionUiModel>>> = emptyList(),
    spendIdr: (TransactionUiModel) -> Double,
): SpendCompare {
    val today = LocalDate.now()
    val curDays = (cycle.end.toEpochDay() - cycle.start.toEpochDay() + 1).toInt().coerceAtLeast(1)
    val prevDays = (prevCycle.end.toEpochDay() - prevCycle.start.toEpochDay() + 1).toInt().coerceAtLeast(1)
    val lastDay = if (today.isAfter(cycle.end)) cycle.end else today
    val todayIndex = (lastDay.toEpochDay() - cycle.start.toEpochDay()).toInt().coerceIn(0, curDays - 1)
    val points = cumulativeByDay(cycle, todayIndex, rows, spendIdr)
    val lastPoints = cumulativeByDay(prevCycle, prevDays - 1, prevRows, spendIdr)
    // ac: spend-compare-more-cycles — older cycles with no spend at the far end are dropped, so the chart never shows a
    // flat line for a cycle from before the app was used. A quiet cycle between two busy ones stays.
    val older = olderCycles
        .map { (range, oldRows) ->
            val days = (range.end.toEpochDay() - range.start.toEpochDay() + 1).toInt().coerceAtLeast(1)
            days to cumulativeByDay(range, days - 1, oldRows, spendIdr)
        }
        .dropLastWhile { (_, curve) -> (curve.lastOrNull()?.cumulative ?: 0.0) <= 0.0 }
    val maxDay = (listOf(curDays, prevDays) + older.map { it.first }).max() - 1
    return SpendCompare(cycle.fromIso, curDays, maxDay, todayIndex, points, lastPoints, older.map { it.second })
}

// Cumulative combined spend for each day of [range], from day 0 through day [upTo] (inclusive).
private fun cumulativeByDay(
    range: PayCycle.Range,
    upTo: Int,
    rows: List<TransactionUiModel>,
    spendIdr: (TransactionUiModel) -> Double,
): List<SpendPoint> {
    val start = range.start
    val days = (range.end.toEpochDay() - start.toEpochDay() + 1).toInt().coerceAtLeast(1)
    val perDay = DoubleArray(days)
    rows.forEach { r ->
        val amt = spendIdr(r)
        if (amt == 0.0) return@forEach
        val d = runCatching { LocalDate.parse(r.dateIso.take(10)) }.getOrNull() ?: return@forEach
        val idx = (d.toEpochDay() - start.toEpochDay()).toInt()
        if (idx in 0 until days) perDay[idx] += amt
    }
    var running = 0.0
    return (0..upTo.coerceIn(0, days - 1)).map { i ->
        running += perDay[i]
        SpendPoint(i, start.plusDays(i.toLong()).toString(), running)
    }
}
