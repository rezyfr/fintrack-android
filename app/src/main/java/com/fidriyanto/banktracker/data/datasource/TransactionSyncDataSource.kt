package com.fidriyanto.banktracker.data.datasource

import com.fidriyanto.banktracker.data.model.TransactionEntry

interface TransactionSyncDataSource {
    suspend fun sync(entry: TransactionEntry): Result<Unit>
}
