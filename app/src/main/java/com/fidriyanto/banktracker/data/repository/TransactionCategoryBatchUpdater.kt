package com.fidriyanto.banktracker.data.repository

import com.fidriyanto.banktracker.data.datasource.TransactionLocalDataSource
import com.fidriyanto.banktracker.data.datasource.TransactionSyncDataSource
import com.fidriyanto.banktracker.data.datasource.remote.mapper.REMOTE_ID_OFFSET

// ac: batch-edit-transaction-category — local ids resync in full; synced ids get one category-only remote PATCH
internal suspend fun batchUpdateCategory(
    ids: Set<Long>,
    category: String,
    localDataSource: TransactionLocalDataSource,
    syncDataSource: TransactionSyncDataSource,
    syncTransaction: suspend (Long) -> Result<Unit>,
): Result<Unit> {
    val (localIds, remoteIds) = ids.partition { it < REMOTE_ID_OFFSET }
    var failure: Throwable? = null
    localIds.forEach { id ->
        localDataSource.getById(id)?.let {
            localDataSource.update(it.copy(category = category))
            syncTransaction(id).onFailure { e -> failure = e }
        }
    }
    if (remoteIds.isNotEmpty()) {
        syncDataSource.updateCategoryBatch(remoteIds.map { it - REMOTE_ID_OFFSET }, category)
            .onFailure { return Result.failure(it) }
    }
    return failure?.let { Result.failure(it) } ?: Result.success(Unit)
}
