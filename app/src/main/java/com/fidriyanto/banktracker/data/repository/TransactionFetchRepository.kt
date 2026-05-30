package com.fidriyanto.banktracker.data.repository

import com.fidriyanto.banktracker.data.db.TransactionEntity

interface TransactionFetchRepository {
    suspend fun fetch(month: String?, wallet: String?, txType: String?): Result<List<TransactionEntity>>
}
