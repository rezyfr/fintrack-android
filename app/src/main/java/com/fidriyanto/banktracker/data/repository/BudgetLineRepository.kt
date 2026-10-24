package com.fidriyanto.banktracker.data.repository

import com.fidriyanto.banktracker.domain.model.BudgetLine

interface BudgetLineRepository {
    suspend fun getAll(): Result<List<BudgetLine>>

    // The app keeps no local copy of the budget lines, so every reader sees a change on its next load.
    suspend fun create(line: BudgetLine): Result<Unit>
    suspend fun update(line: BudgetLine): Result<Unit>
    suspend fun delete(id: Long): Result<Unit>
}
