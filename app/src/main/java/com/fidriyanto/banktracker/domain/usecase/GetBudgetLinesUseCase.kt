package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.repository.BudgetLineRepository
import com.fidriyanto.banktracker.domain.model.BudgetLine
import javax.inject.Inject
import javax.inject.Singleton

// ac: budget-by-category — the active budget lines, so the transactions list can mark an expense that
// counts toward no budget
@Singleton
class GetBudgetLinesUseCase @Inject constructor(
    private val budgetLineRepository: BudgetLineRepository,
) {
    suspend operator fun invoke(): Result<List<BudgetLine>> = budgetLineRepository.getAll()
}
