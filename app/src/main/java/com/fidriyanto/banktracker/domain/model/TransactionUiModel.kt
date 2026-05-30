package com.fidriyanto.banktracker.domain.model

import com.fidriyanto.banktracker.data.model.TransactionStatus

data class TransactionUiModel(
    val id: Long,
    val merchant: String,
    val item: String,
    val category: String,
    val amount: Double,
    val dateIso: String,
    val wallet: String?,
    val txType: String,
    val status: TransactionStatus
)
