package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.repository.TransactionRepository
import com.fidriyanto.banktracker.domain.model.CategoryRow
import com.fidriyanto.banktracker.domain.model.CurrencySummary
import com.fidriyanto.banktracker.domain.model.DashboardSummaryPair
import com.fidriyanto.banktracker.domain.model.TransactionUiModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

private val WALLET_CURRENCIES = mapOf(
    "BBL" to "THB", "BCA" to "IDR", "BCA_CC" to "IDR",
    "MANDIRI" to "IDR", "MANDIRI_CC" to "IDR", "INVESTMENT" to "IDR",
)

// Only BBL is THB; everything else is IDR.
internal fun walletCurrencyOf(wallet: String?): String = WALLET_CURRENCIES[wallet] ?: "IDR"

// ac: insights-filter-by-wallet — when a single wallet is selected, fetch raw transactions and aggregate
internal fun observeByWallet(
    transactionRepository: TransactionRepository,
    monthPrefixes: List<String>,
    wallet: String,
): Flow<DashboardSummaryPair> = flow {
    val allTx = mutableListOf<TransactionUiModel>()
    for (ym in monthPrefixes) {
        transactionRepository.fetch(ym, wallet, null).onSuccess { allTx.addAll(it) }
    }
    val summary = aggregateTransactions(allTx)
    val walletCurrency = WALLET_CURRENCIES[wallet] ?: "IDR"
    val empty = CurrencySummary(0.0, 0.0, 0.0, emptyList())
    // ac: insights-filter-by-wallet — when a single wallet is selected only the relevant currency section is shown
    if (walletCurrency == "THB") emit(DashboardSummaryPair(thb = summary, idr = empty))
    else emit(DashboardSummaryPair(thb = empty, idr = summary))
}

private fun aggregateTransactions(transactions: List<TransactionUiModel>): CurrencySummary {
    val income = transactions.filter { it.txType == "income" }.sumOf { it.amount }
    val expenses = transactions.filter { it.txType == "expense" }.sumOf { it.amount }
    val catMap = transactions.filter { it.txType == "expense" }
        .groupBy { it.category }
        .mapValues { (_, txs) -> txs.sumOf { it.amount } }
        .filter { it.value > 0.0 }
        .toList()
        .sortedByDescending { it.second }
        .map { (cat, amt) -> CategoryRow(cat, amt, if (expenses > 0.0) (amt / expenses).toFloat() else 0f) }
    // ac: insights-subcategory-breakdown — the wallet-filtered summary also carries the subcategory breakdown
    return CurrencySummary(income, expenses, income - expenses, catMap, subcategoryBreakdown = buildSubcategoryBreakdown(transactions))
}
