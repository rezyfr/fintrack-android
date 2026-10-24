package com.fidriyanto.banktracker.domain.model

import com.fidriyanto.banktracker.data.model.TransactionStatus

data class TransactionUiModel(
    val id: Long,
    val item: String,
    val category: String,
    val amount: Double,
    val dateIso: String,
    val wallet: String?,
    val txType: String,
    val status: TransactionStatus,
    val subcategory: String? = null,
    val toWallet: String? = null,
    val toAmount: Double? = null,
    val budgetLineId: Long? = null,
    // ac: installment-link-edit — the installment this transaction pays a step of, when linked
    val installmentId: Long? = null,
)
