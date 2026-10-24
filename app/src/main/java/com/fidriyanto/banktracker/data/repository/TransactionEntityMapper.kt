package com.fidriyanto.banktracker.data.repository

import com.fidriyanto.banktracker.data.db.TransactionEntity
import com.fidriyanto.banktracker.data.model.TransactionEntry
import java.time.LocalDate
import com.fidriyanto.banktracker.domain.model.TransactionUiModel

internal fun TransactionEntity.toUiModel() = TransactionUiModel(
    id = id, item = item, category = category,
    amount = amount, dateIso = dateIso, wallet = wallet,
    txType = txType ?: "expense", status = status,
    subcategory = subcategory,
    toWallet = toWallet,
    toAmount = toAmount,
    budgetLineId = budgetLineId,
    installmentId = installmentId,
)

internal fun TransactionEntity.toEntry() = TransactionEntry(
    tab = tab, date = LocalDate.parse(dateIso), item = item, amount = amount, category = category,
    wallet = wallet, txType = txType, toWallet = toWallet, subcategory = subcategory, toAmount = toAmount,
    installmentId = installmentId,
)
