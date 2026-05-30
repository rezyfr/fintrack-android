package com.fidriyanto.banktracker.data.datasource

import android.util.Log
import com.fidriyanto.banktracker.data.datasource.remote.SupabaseTransactionService
import com.fidriyanto.banktracker.data.datasource.remote.mapper.toInsertDto
import com.fidriyanto.banktracker.data.model.TransactionEntry
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionSyncDataSourceImpl @Inject constructor(
    private val service: SupabaseTransactionService
) : TransactionSyncDataSource {

    override suspend fun sync(entry: TransactionEntry): Result<Unit> = runCatching {
        val body = entry.toInsertDto()
        Log.d("TransactionSyncDS", "POST transactions: merchant=${entry.merchant} amount=${entry.amount}")
        val response = service.insertTransaction(body)
        Log.d("TransactionSyncDS", "status=${response.code()}")
        if (!response.isSuccessful) error("Supabase error: HTTP ${response.code()}")
    }.onFailure { Log.e("TransactionSyncDS", "sync failed", it) }
}
