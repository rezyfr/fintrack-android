package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.repository.BudgetLineRepository
import com.fidriyanto.banktracker.domain.model.BudgetLine
import javax.inject.Inject

// Read, save and delete the budget lines. A line with id 0 is new.
class ManageBudgetLinesUseCase @Inject constructor(
    private val budgetLines: BudgetLineRepository,
) {
    suspend fun list(): Result<List<BudgetLine>> = budgetLines.getAll()

    suspend fun delete(id: Long): Result<Unit> = budgetLines.delete(id)

    // A new line gets the sort order after the highest one. The list is read again, so a line added
    // elsewhere is counted.
    suspend fun save(line: BudgetLine): Result<Unit> {
        if (line.id != 0L) return budgetLines.update(line)
        val order = BudgetLineRules.nextSortOrder(budgetLines.getAll().getOrElse { return Result.failure(it) })
        return budgetLines.create(line.copy(sortOrder = order))
    }
}
