package com.fidriyanto.banktracker.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fidriyanto.banktracker.data.model.SheetTab
import com.fidriyanto.banktracker.data.model.TransactionStatus

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val merchant: String,
    val item: String,
    val amount: Double,
    val category: String,
    val dateIso: String,
    val channel: String,
    val referenceNo: String,
    val tab: SheetTab = SheetTab.EXPENSES,
    val status: TransactionStatus = TransactionStatus.PENDING_EDIT,
    val createdAt: Long = System.currentTimeMillis(),
    val wallet: String? = null,      // 'BBL' | 'BCA' | 'MANDIRI' | 'MANDIRI_CC' | 'INVESTMENT'
    val txType: String = "expense",  // 'expense' | 'income' | 'transfer' | 'investment'
    val toWallet: String? = null     // only populated for txType = 'transfer'
)
