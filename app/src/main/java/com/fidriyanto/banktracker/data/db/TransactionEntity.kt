package com.fidriyanto.banktracker.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fidriyanto.banktracker.data.model.LedgerTab
import com.fidriyanto.banktracker.data.model.TransactionStatus

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val merchant: String,
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
    val toWallet: String? = null
)
