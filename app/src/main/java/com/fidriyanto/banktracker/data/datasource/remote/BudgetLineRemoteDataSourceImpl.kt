package com.fidriyanto.banktracker.data.datasource.remote

import android.util.Log
import com.fidriyanto.banktracker.data.datasource.remote.dto.BudgetLineDto
import com.fidriyanto.banktracker.data.datasource.remote.mapper.toCreateBody
import com.fidriyanto.banktracker.data.datasource.remote.mapper.toUpdateBody
import com.fidriyanto.banktracker.domain.model.BudgetLine
import javax.inject.Inject

class BudgetLineRemoteDataSourceImpl @Inject constructor(
    private val service: BudgetLineApiService,
) : BudgetLineRemoteDataSource {
    override suspend fun getBudgetLines(): List<BudgetLineDto> = service.getBudgetLines()

    override suspend fun create(line: BudgetLine): Result<Unit> = runCatching {
        Log.d(TAG, "POST budget-lines: name=${line.name}")
        val response = service.createBudgetLine(line.toCreateBody())
        Log.d(TAG, "status=${response.code()}")
        if (!response.isSuccessful) error("API error: HTTP ${response.code()}")
    }.onFailure { Log.e(TAG, "create failed", it) }

    override suspend fun update(line: BudgetLine): Result<Unit> = runCatching {
        Log.d(TAG, "PATCH budget-lines: id=${line.id}")
        val response = service.updateBudgetLine(line.id, line.toUpdateBody())
        Log.d(TAG, "status=${response.code()}")
        if (!response.isSuccessful) error("API error: HTTP ${response.code()}")
    }.onFailure { Log.e(TAG, "update failed", it) }

    override suspend fun delete(id: Long): Result<Unit> = runCatching {
        Log.d(TAG, "DELETE budget-lines: id=$id")
        val response = service.deleteBudgetLine(id)
        Log.d(TAG, "status=${response.code()}")
        // A line that is already gone is deleted, so a retry after a lost answer succeeds.
        if (!response.isSuccessful && response.code() != 404) error("API error: HTTP ${response.code()}")
    }.onFailure { Log.e(TAG, "delete failed", it) }

    private companion object {
        const val TAG = "BudgetLineRemoteDS"
    }
}
