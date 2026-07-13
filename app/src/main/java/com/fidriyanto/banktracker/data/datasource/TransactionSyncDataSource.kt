package com.fidriyanto.banktracker.data.datasource

import com.fidriyanto.banktracker.data.model.TransactionEdit
import com.fidriyanto.banktracker.data.model.TransactionEntry

interface TransactionSyncDataSource {
    suspend fun sync(entry: TransactionEntry): Result<Unit>
    suspend fun delete(id: Long): Result<Unit>
    suspend fun update(id: Long, edit: TransactionEdit): Result<Unit>
    // ac: batch-edit-transaction-category — one PATCH updates the category of every remote id at once
    suspend fun updateCategoryBatch(remoteIds: List<Long>, category: String): Result<Unit>
}
