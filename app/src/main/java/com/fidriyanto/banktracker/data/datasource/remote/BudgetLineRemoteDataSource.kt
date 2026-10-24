package com.fidriyanto.banktracker.data.datasource.remote

import com.fidriyanto.banktracker.data.datasource.remote.dto.BudgetLineDto
import com.fidriyanto.banktracker.domain.model.BudgetLine

interface BudgetLineRemoteDataSource {
    suspend fun getBudgetLines(): List<BudgetLineDto>
    suspend fun create(line: BudgetLine): Result<Unit>
    suspend fun update(line: BudgetLine): Result<Unit>
    suspend fun delete(id: Long): Result<Unit>
}
