package com.fidriyanto.banktracker.data.datasource

import android.util.Log
import com.fidriyanto.banktracker.data.datasource.remote.SupabaseTransactionService
import com.fidriyanto.banktracker.data.datasource.remote.dto.TransactionPatchDto
import com.fidriyanto.banktracker.data.datasource.remote.mapper.toInsertDto
import com.fidriyanto.banktracker.data.model.TransactionEdit
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

    override suspend fun delete(id: Long): Result<Unit> = runCatching {
        Log.d("TransactionSyncDS", "DELETE transactions: id=$id")
        val response = service.deleteTransaction("eq.$id")
        Log.d("TransactionSyncDS", "status=${response.code()}")
        if (!response.isSuccessful) error("Supabase error: HTTP ${response.code()}")
    }.onFailure { Log.e("TransactionSyncDS", "delete failed", it) }

    override suspend fun update(id: Long, edit: TransactionEdit): Result<Unit> = runCatching {
        // ac: edit-transfer-target-amount — saving the edit persists the entered to_amount value to Supabase
        val body = TransactionPatchDto(
            amount   = edit.amount,
            item     = edit.item,
            category = edit.category,
            date     = edit.dateIso,
            wallet   = edit.wallet,
            txType   = edit.txType,
            toWallet = edit.toWallet,
            toAmount = edit.toAmount,
        )
        Log.d("TransactionSyncDS", "PATCH transactions: id=$id item=${edit.item}")
        val response = service.updateTransaction("eq.$id", body)
        Log.d("TransactionSyncDS", "status=${response.code()}")
        if (!response.isSuccessful) error("Supabase error: HTTP ${response.code()}")
    }.onFailure { Log.e("TransactionSyncDS", "update failed", it) }
}
