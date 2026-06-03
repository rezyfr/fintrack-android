package com.fidriyanto.banktracker.data.datasource

import com.fidriyanto.banktracker.data.db.TransactionEntity

interface TransactionFetchDataSource {
    suspend fun fetch(month: String?, wallet: String?, txType: String?, category: String? = null): Result<List<TransactionEntity>>
}
