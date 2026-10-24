package com.fidriyanto.banktracker.data.datasource.offline

import com.fidriyanto.banktracker.data.datasource.TransactionSyncDataSource
import com.fidriyanto.banktracker.data.db.LedgerDao
import com.fidriyanto.banktracker.data.db.LedgerTransactionEntity
import com.fidriyanto.banktracker.data.model.TransactionEdit
import com.fidriyanto.banktracker.data.model.TransactionEntry
import javax.inject.Inject
import javax.inject.Singleton

// ac: offline-mode - writes transactions to the offline store with the rules of the server: an account is
// required, a linked installment must exist, a delete of a row that is gone succeeds, and an edit keeps the
// fields that it leaves empty.
@Singleton
class OfflineTransactionSyncDataSource @Inject constructor(
    private val dao: LedgerDao,
) : TransactionSyncDataSource {
    override suspend fun sync(entry: TransactionEntry): Result<Unit> = runCatching {
        val wallet = requireNotNull(entry.wallet) { "A transaction needs an account" }
        entry.installmentId?.let { requireNotNull(dao.installment(it)) { "Installment $it not found" } }
        dao.insertTransaction(
            LedgerTransactionEntity(
                item = entry.item, amount = entry.amount, category = entry.category, subcategory = entry.subcategory,
                date = entry.date.toString(), tab = entry.tab.name, note = entry.note, wallet = wallet,
                txType = entry.txType, toWallet = entry.toWallet, toAmount = entry.toAmount,
                installmentId = entry.installmentId, createdAt = System.currentTimeMillis(),
            ),
        )
    }

    override suspend fun delete(id: Long): Result<Unit> = runCatching {
        dao.deleteTransactions(listOf(id))
    }

    override suspend fun update(id: Long, edit: TransactionEdit): Result<Unit> = runCatching {
        val row = requireNotNull(dao.transaction(id)) { "Transaction $id not found" }
        edit.installmentId?.let { requireNotNull(dao.installment(it)) { "Installment $it not found" } }
        dao.updateTransaction(
            row.copy(
                amount = edit.amount, item = edit.item, category = edit.category, date = edit.dateIso,
                wallet = edit.wallet ?: row.wallet, txType = edit.txType, toWallet = edit.toWallet ?: row.toWallet,
                toAmount = edit.toAmount ?: row.toAmount, subcategory = edit.subcategory ?: row.subcategory,
                installmentId = edit.installmentId ?: row.installmentId,
            ),
        )
    }

    override suspend fun unlinkInstallment(id: Long): Result<Unit> = runCatching {
        val row = requireNotNull(dao.transaction(id)) { "Transaction $id not found" }
        dao.updateTransaction(row.copy(installmentId = null))
    }

    override suspend fun updateCategoryBatch(remoteIds: List<Long>, category: String): Result<Unit> = runCatching {
        dao.setCategory(remoteIds, category)
    }
}
