package com.fidriyanto.banktracker.data.model

import java.time.LocalDate

data class TransactionEntry(
    val tab: LedgerTab,
    val date: LocalDate,
    val item: String,
    val amount: Double,
    val category: String,
    val note: String? = null,
    val wallet: String? = null,
    val txType: String = "expense",
    val toWallet: String? = null,
    val subcategory: String? = null,
    val toAmount: Double? = null,
    // ac: installment-auto-link — the installment this card transaction pays a step of
    val installmentId: Long? = null,
)
