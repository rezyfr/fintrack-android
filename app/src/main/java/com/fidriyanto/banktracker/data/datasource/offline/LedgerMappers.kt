package com.fidriyanto.banktracker.data.datasource.offline

import com.fidriyanto.banktracker.data.datasource.remote.dto.BudgetLineDto
import com.fidriyanto.banktracker.data.datasource.remote.dto.CardBillingDto
import com.fidriyanto.banktracker.data.datasource.remote.dto.InstallmentDto
import com.fidriyanto.banktracker.data.datasource.remote.dto.TransactionDto
import com.fidriyanto.banktracker.data.db.LedgerBudgetLineEntity
import com.fidriyanto.banktracker.data.db.LedgerCardBillingEntity
import com.fidriyanto.banktracker.data.db.LedgerInstallmentEntity
import com.fidriyanto.banktracker.data.db.LedgerTransactionEntity
import com.fidriyanto.banktracker.data.model.BackupTransaction

// ac: offline-mode - the offline store answers with the same DTOs as the server, so the existing mappers
// and repositories work unchanged.

fun LedgerTransactionEntity.toDto() = TransactionDto(
    id = id, item = item, amount = amount, category = category, date = date, tab = tab, wallet = wallet,
    txType = txType, toWallet = toWallet, toAmount = toAmount, subcategory = subcategory,
    budgetLineId = budgetLineId, installmentId = installmentId,
)

fun LedgerInstallmentEntity.toDto() = InstallmentDto(
    id = id, merchant = merchant, merchantPattern = merchantPattern, wallet = wallet,
    installmentAmount = installmentAmount, totalInstallments = totalInstallments, currentStep = currentStep,
    dueDay = dueDay, status = status, excluded = excluded,
)

fun LedgerBudgetLineEntity.toDto() = BudgetLineDto(
    id = id, name = name, kind = kind, currency = currency, target = target, matchWallets = matchWallets,
    matchPattern = matchPattern, matchCategories = matchCategories, sortOrder = sortOrder, active = active,
    category = category, subcategory = subcategory, dueDay = dueDay,
)

fun LedgerCardBillingEntity.toDto() = CardBillingDto(
    wallet = wallet, cutoffDay = cutoffDay, dueDay = dueDay, minPercent = minPercent,
    minFullInstallments = minFullInstallments,
)

// ac: offline-backup - the backup keeps every field of the offline store, so an import gives back the same rows.

fun LedgerTransactionEntity.toBackup() = BackupTransaction(
    id = id, item = item, amount = amount, category = category, subcategory = subcategory, date = date, tab = tab,
    note = note, wallet = wallet, txType = txType, toWallet = toWallet, toAmount = toAmount,
    budgetLineId = budgetLineId, installmentId = installmentId, createdAt = createdAt,
)

fun BackupTransaction.toLedger() = LedgerTransactionEntity(
    id = id, item = item, amount = amount, category = category, subcategory = subcategory, date = date, tab = tab,
    note = note, wallet = wallet, txType = txType, toWallet = toWallet, toAmount = toAmount,
    budgetLineId = budgetLineId, installmentId = installmentId, createdAt = createdAt,
)

fun InstallmentDto.toLedger() = LedgerInstallmentEntity(
    id = id, merchant = merchant, merchantPattern = merchantPattern, wallet = wallet,
    installmentAmount = installmentAmount, totalInstallments = totalInstallments, currentStep = currentStep,
    dueDay = dueDay, status = status, excluded = excluded,
)

fun BudgetLineDto.toLedger() = LedgerBudgetLineEntity(
    id = id, name = name, kind = kind, currency = currency, target = target, matchWallets = matchWallets,
    matchPattern = matchPattern, matchCategories = matchCategories, sortOrder = sortOrder, active = active,
    category = category, subcategory = subcategory, dueDay = dueDay,
)

fun CardBillingDto.toLedger() = LedgerCardBillingEntity(
    wallet = wallet, cutoffDay = cutoffDay, dueDay = dueDay, minPercent = minPercent,
    minFullInstallments = minFullInstallments,
)
