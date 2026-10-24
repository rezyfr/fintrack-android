package com.fidriyanto.banktracker.data.db

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.fidriyanto.banktracker.data.model.LedgerTab
import com.fidriyanto.banktracker.data.model.TransactionStatus

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val item: String,
    val amount: Double,
    val category: String,
    val dateIso: String,
    val referenceNo: String,
    val tab: LedgerTab = LedgerTab.EXPENSES,
    val status: TransactionStatus = TransactionStatus.PENDING_EDIT,
    val createdAt: Long = System.currentTimeMillis(),
    val wallet: String? = null,
    val txType: String = "expense",
    val toWallet: String? = null,
    val subcategory: String? = null,
    val toAmount: Double? = null
) {
    // ac: budget-line-details-and-assignment — the fetched manual budget assignment, carried through
    // to attribution. Not persisted (no schema change), only set on rows read from the server.
    @Ignore
    var budgetLineId: Long? = null

    // ac: installment-link-edit — the installment link of a row read from the server. Not persisted.
    @Ignore
    var installmentId: Long? = null
}
