package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.repository.BudgetLineRepository
import com.fidriyanto.banktracker.data.repository.TransactionRepository
import com.fidriyanto.banktracker.domain.model.BudgetLineSpend
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

data class BudgetGlance(
    val currency: String,
    val totalTarget: Double,
    val totalSpent: Double,
    val remaining: Double,
    val daysLeft: Int,
    val dailyAllowance: Double,
    val lines: List<BudgetLineSpend>,
)

@Singleton
class GetBudgetGlanceUseCase @Inject constructor(
    private val budgetLineRepository: BudgetLineRepository,
    private val transactionRepository: TransactionRepository,
) {
    // Fallback rate when none is derivable; matches the session's working IDR/THB rate.
    private val fallbackRate = 535.0

    // ac: android-budget-daily-allowance — flex lines for one currency over the current pay cycle,
    // with a safe daily allowance of remaining / days left.
    suspend fun getGlance(currency: String): Result<BudgetGlance> = runCatching {
        val cycle = PayCycle.current()
        val allLines = budgetLineRepository.getAll().getOrThrow()
        val flex = allLines.filter { it.kind == "flex" && it.currency == currency }
        val rows = transactionRepository
            .fetch(month = null, wallet = null, txType = null, dateFrom = cycle.fromIso, dateTo = cycle.toIso)
            .getOrElse { emptyList() }
        // Attribute against ALL lines (first-match-wins across fixed+flex), then keep this currency's flex.
        val spend = BudgetAttribution.spendByLine(rows, allLines, fallbackRate)
        val lineSpends = flex
            .sortedByDescending { it.target }
            .map { BudgetLineSpend(it, spend[it.id] ?: 0.0) }
        val totalTarget = lineSpends.sumOf { it.line.target }
        val totalSpent = lineSpends.sumOf { it.spent }
        val remaining = (totalTarget - totalSpent).coerceAtLeast(0.0)
        val daysLeft = daysLeftInCycle(cycle.end)
        BudgetGlance(
            currency = currency,
            totalTarget = totalTarget,
            totalSpent = totalSpent,
            remaining = remaining,
            daysLeft = daysLeft,
            dailyAllowance = if (daysLeft > 0) remaining / daysLeft else remaining,
            lines = lineSpends,
        )
    }

    // Days remaining in the cycle, counting today, at least 1.
    private fun daysLeftInCycle(end: LocalDate, today: LocalDate = LocalDate.now()): Int =
        ((end.toEpochDay() - today.toEpochDay()).toInt() + 1).coerceAtLeast(1)
}
