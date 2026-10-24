package com.fidriyanto.banktracker.data.datasource

import android.util.Log
import com.fidriyanto.banktracker.data.datasource.remote.TransactionApiService
import com.fidriyanto.banktracker.data.datasource.remote.dto.CategoryBatchPatchDto
import com.fidriyanto.banktracker.data.datasource.remote.dto.TransactionPatchDto
import com.fidriyanto.banktracker.data.datasource.remote.mapper.clearInstallmentBody
import com.fidriyanto.banktracker.data.datasource.remote.mapper.toInsertDto
import com.fidriyanto.banktracker.data.model.TransactionEdit
import com.fidriyanto.banktracker.data.model.TransactionEntry
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionSyncDataSourceImpl @Inject constructor(
    private val service: TransactionApiService
) : TransactionSyncDataSource {
    override suspend fun sync(entry: TransactionEntry): Result<Unit> = runCatching {
        val body = entry.toInsertDto()
        Log.d("TransactionSyncDS", "POST transactions: item=${entry.item} amount=${entry.amount}")
        val response = service.insertTransaction(body)
        Log.d("TransactionSyncDS", "status=${response.code()}")
        if (!response.isSuccessful) error("API error: HTTP ${response.code()}")
    }.onFailure { Log.e("TransactionSyncDS", "sync failed", it) }

    override suspend fun delete(id: Long): Result<Unit> = runCatching {
        Log.d("TransactionSyncDS", "DELETE transactions: id=$id")
        val response = service.deleteTransaction(id)
        Log.d("TransactionSyncDS", "status=${response.code()}")
        // A row that is already gone is deleted, so a retry after a lost answer succeeds.
        if (!response.isSuccessful && response.code() != 404) error("API error: HTTP ${response.code()}")
    }.onFailure { Log.e("TransactionSyncDS", "delete failed", it) }

    override suspend fun update(id: Long, edit: TransactionEdit): Result<Unit> = runCatching {
        // ac: edit-transfer-target-amount — saving the edit persists the entered to_amount value to the server
        val body = TransactionPatchDto(
            amount = edit.amount,
            item = edit.item,
            category = edit.category,
            date = edit.dateIso,
            wallet = edit.wallet,
            txType = edit.txType,
            toWallet = edit.toWallet,
            toAmount = edit.toAmount,
            subcategory = edit.subcategory,
            installmentId = edit.installmentId,
        )
        Log.d("TransactionSyncDS", "PATCH transactions: id=$id item=${edit.item}")
        val response = service.updateTransaction(id, body)
        Log.d("TransactionSyncDS", "status=${response.code()}")
        if (!response.isSuccessful) error("API error: HTTP ${response.code()}")
    }.onFailure { Log.e("TransactionSyncDS", "update failed", it) }

    // ac: installment-link-edit — a dedicated PATCH, because a plain PATCH body drops the null field
    override suspend fun unlinkInstallment(id: Long): Result<Unit> = runCatching {
        Log.d("TransactionSyncDS", "PATCH transactions installment_id=null: id=$id")
        val response = service.clearInstallment(id, clearInstallmentBody())
        if (!response.isSuccessful) error("API error: HTTP ${response.code()}")
    }.onFailure { Log.e("TransactionSyncDS", "unlinkInstallment failed", it) }

    // ac: batch-edit-transaction-category — single PATCH for the listed ids, only the category changes
    override suspend fun updateCategoryBatch(remoteIds: List<Long>, category: String): Result<Unit> = runCatching {
        Log.d("TransactionSyncDS", "PATCH transactions category batch: ids=$remoteIds")
        val response = service.updateCategoryBatch(CategoryBatchPatchDto(remoteIds, category))
        Log.d("TransactionSyncDS", "status=${response.code()}")
        if (!response.isSuccessful) error("API error: HTTP ${response.code()}")
    }.onFailure { Log.e("TransactionSyncDS", "updateCategoryBatch failed", it) }
}
