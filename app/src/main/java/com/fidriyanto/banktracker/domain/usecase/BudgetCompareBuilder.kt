package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.domain.model.BudgetLine
import com.fidriyanto.banktracker.domain.model.SpendCompare
import com.fidriyanto.banktracker.domain.model.TransactionUiModel

internal data class BudgetCompare(val all: SpendCompare?, val byLine: Map<Long, SpendCompare>)

// ac: budget-spend-comparison-chart — cumulative spend this cycle vs last cycle for all flexible lines
// together and for each line. agg and prevAgg are the attributed rows of the two cycles. amountOf gives
// a row's amount in its line's currency. Row ids are unique across both cycles, so one map serves both.
internal fun buildBudgetCompare(
    cycle: PayCycle.Range,
    prevCycle: PayCycle.Range,
    flex: List<BudgetLine>,
    agg: Map<Long, BudgetAttribution.LineAgg>,
    prevAgg: Map<Long, BudgetAttribution.LineAgg>,
    olderCycles: List<Pair<PayCycle.Range, Map<Long, BudgetAttribution.LineAgg>>> = emptyList(),
    mergeSameName: Boolean = false,
    amountOf: (TransactionUiModel, BudgetLine) -> Double,
): BudgetCompare {
    val amountById = HashMap<Long, Double>()
    flex.forEach { line ->
        (listOf(agg, prevAgg) + olderCycles.map { it.second }).forEach { a ->
            a[line.id]?.rows?.forEach { amountById[it.id] = amountOf(it, line) }
        }
    }
    val rowAmount: (TransactionUiModel) -> Double = { amountById[it.id] ?: 0.0 }
    fun compareFor(lines: List<BudgetLine>) = buildSpendCompare(
        cycle, prevCycle,
        lines.flatMap { agg[it.id]?.rows ?: emptyList() },
        lines.flatMap { prevAgg[it.id]?.rows ?: emptyList() },
        olderCycles.map { (range, oldAgg) -> range to lines.flatMap { oldAgg[it.id]?.rows ?: emptyList() } },
        rowAmount,
    )
    return BudgetCompare(
        all = if (flex.isEmpty()) null else compareFor(flex),
        // ac: budget-combined-chart-merge — in the combined view one category has a line per currency. Each
        // of them shows the same chart: the lines of that name added together in the home currency.
        byLine = flex.associate { line ->
            line.id to compareFor(if (mergeSameName) flex.filter { it.name == line.name } else listOf(line))
        },
    )
}
