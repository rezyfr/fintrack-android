package com.fidriyanto.banktracker.data.repository

import com.fidriyanto.banktracker.domain.model.BudgetLine

interface BudgetLineRepository {
    suspend fun getAll(): Result<List<BudgetLine>>
}
