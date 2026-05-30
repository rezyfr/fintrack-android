package com.fidriyanto.banktracker.data.model

import java.time.LocalDate

data class TransactionEntry(
    val tab: LedgerTab,
    val date: LocalDate,
    val merchant: String,
    val item: String,
    val amount: Double,
    val category: String,
    val note: String? = null,
    val wallet: String? = null,
    val txType: String = "expense",
    val toWallet: String? = null
)
