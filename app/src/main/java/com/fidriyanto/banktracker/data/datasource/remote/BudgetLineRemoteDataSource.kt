package com.fidriyanto.banktracker.data.datasource.remote

import com.fidriyanto.banktracker.data.datasource.remote.dto.BudgetLineDto

interface BudgetLineRemoteDataSource {
    suspend fun getBudgetLines(): List<BudgetLineDto>
}
