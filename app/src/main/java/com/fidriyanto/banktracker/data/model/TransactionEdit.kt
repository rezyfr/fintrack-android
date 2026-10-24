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
    // ac: installment-link-edit — an installment the user picked by hand, or null for no change
    val installmentId: Long? = null,
    // ac: installment-link-edit — true when the user removed the transaction's installment link
    val unlinkInstallment: Boolean = false,
    // ac: installment-link-edit — true for an unlinked transaction, so a matching installment is linked on save
    val autoLinkInstallment: Boolean = false,
)
