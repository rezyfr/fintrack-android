package com.fidriyanto.banktracker.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// ac: offline-mode - a transaction in offline mode. The table holds the same fields as the transactions
// table of the FinTrack server, so the phone answers the same questions the server does.
@Entity(
    tableName = "ledger_transactions",
    indices = [Index("date"), Index("wallet"), Index("toWallet"), Index("installmentId")],
)
data class LedgerTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val item: String,
    val amount: Double,
    val category: String,
    val subcategory: String? = null,
    // yyyy-MM-dd, so a text comparison orders the days
    val date: String,
    val tab: String,
    val note: String? = null,
    val wallet: String,
    val txType: String,
    val toWallet: String? = null,
    val toAmount: Double? = null,
    val budgetLineId: Long? = null,
    val installmentId: Long? = null,
    val createdAt: Long = 0,
)
