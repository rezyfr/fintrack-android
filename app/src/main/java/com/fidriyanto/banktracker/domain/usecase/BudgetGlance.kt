package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.domain.model.BudgetLineSpend
import com.fidriyanto.banktracker.domain.model.SpendCompare
import com.fidriyanto.banktracker.domain.model.TransactionUiModel

data class BudgetGlance(
    val currency: String,
    val totalTarget: Double,
    val totalSpent: Double,
    val remaining: Double,
    val daysLeft: Int,
    val dailyAllowance: Double,
    val lines: List<BudgetLineSpend>,
    // ac: budget-fixed-bills — fixed obligations for this currency with their cycle spend
    val fixed: List<BudgetLineSpend> = emptyList(),
    // ac: budget-spend-comparison-chart — cumulative spend this cycle vs last cycle in the budget's
    // currency, for all flexible lines together and for each flexible line by id
    val allCompare: SpendCompare? = null,
    // ac: budget-combined-chart-merge — the combined view has one chart chip per line name, not per currency
    val mergedChips: Boolean = false,
    val lineCompare: Map<Long, SpendCompare> = emptyMap(),
    // ac: budget-cycle-stepper — false for a past cycle, which has no days left
    val isCurrentCycle: Boolean = true,
    // ac: budget-unbudgeted-line — expense spend that no line claims, and its latest rows
    val unbudgetedSpent: Double = 0.0,
    val unbudgetedRecent: List<TransactionUiModel> = emptyList(),
    // ac: budget-cap-at-balance — the checking-account balance in this view's currency, for the cycle in
    // progress only. dailyAllowance never spends more than this.
    val balance: Double? = null,
)
