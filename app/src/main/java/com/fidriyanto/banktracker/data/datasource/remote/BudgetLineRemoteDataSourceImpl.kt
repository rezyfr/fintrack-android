package com.fidriyanto.banktracker.data.datasource.remote

import com.fidriyanto.banktracker.data.datasource.remote.dto.BudgetLineDto
import javax.inject.Inject

class BudgetLineRemoteDataSourceImpl @Inject constructor(
    private val service: SupabaseBudgetLineService,
) : BudgetLineRemoteDataSource {
    override suspend fun getBudgetLines(): List<BudgetLineDto> = service.getBudgetLines()
}
