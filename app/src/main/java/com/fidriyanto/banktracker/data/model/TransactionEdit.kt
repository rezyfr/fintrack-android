package com.fidriyanto.banktracker.data.model

data class TransactionEdit(
    val amount: Double,
    val item: String,
    val category: String,
    val dateIso: String,
    val wallet: String?,
    val txType: String,
    val toWallet: String?,
    // ac: edit-transfer-target-amount — nullable to_amount for cross-currency transfers
    val toAmount: Double? = null,
    val subcategory: String? = null,
)
