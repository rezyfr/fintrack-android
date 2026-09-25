package com.fidriyanto.banktracker.data.repository

import com.fidriyanto.banktracker.data.db.TransactionEntity
import com.fidriyanto.banktracker.domain.model.TransactionUiModel

internal fun TransactionEntity.toUiModel() = TransactionUiModel(
    id = id, item = item, category = category,
    amount = amount, dateIso = dateIso, wallet = wallet,
    txType = txType ?: "expense", status = status,
    subcategory = subcategory,
)
