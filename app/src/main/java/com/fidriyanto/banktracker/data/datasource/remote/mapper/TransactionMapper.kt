package com.fidriyanto.banktracker.data.datasource.remote.mapper

import com.fidriyanto.banktracker.data.datasource.remote.dto.TransactionDto
import com.fidriyanto.banktracker.data.datasource.remote.dto.TransactionInsertDto
import com.fidriyanto.banktracker.data.db.TransactionEntity
import com.fidriyanto.banktracker.data.model.LedgerTab
import com.fidriyanto.banktracker.data.model.TransactionEntry
import com.fidriyanto.banktracker.data.model.TransactionStatus

private const val REMOTE_ID_OFFSET = 10_000_000L

fun TransactionDto.toEntity(): TransactionEntity? = runCatching {
    TransactionEntity(
        id          = id + REMOTE_ID_OFFSET,
        merchant    = merchant.orEmpty(),
        item        = item.orEmpty(),
        amount      = amount,
        category    = category ?: "Other",
        dateIso     = date.orEmpty(),
        referenceNo = "",
        tab         = tab?.let { runCatching { LedgerTab.valueOf(it) }.getOrDefault(LedgerTab.EXPENSES) }
                    ?: LedgerTab.EXPENSES,
        status      = TransactionStatus.SYNCED,
        wallet      = wallet,
        txType      = txType ?: "expense",
        toWallet    = toWallet,
    )
}.getOrNull()

fun TransactionEntry.toInsertDto() = TransactionInsertDto(
    tab      = tab.name,
    date     = date.toString(),
    merchant = merchant,
    item     = item,
    amount   = amount,
    category = category,
    note     = note,
    wallet   = wallet,
    txType   = txType,
    toWallet = toWallet
)
