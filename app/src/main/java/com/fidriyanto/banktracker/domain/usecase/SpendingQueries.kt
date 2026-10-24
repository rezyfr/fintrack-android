package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.domain.model.TransactionUiModel
import com.fidriyanto.banktracker.domain.model.formatMoney

// Plain sums over fetched rows. The model asks, and these functions count, so the numbers are exact.
object SpendingQueries {
    // Expense rows only. A category filter matches the category or the subcategory, ignoring case.
    // A part of the name is enough, because a small model says "Food" for "Food & Drink".
    fun expenses(rows: List<TransactionUiModel>, category: String?): List<TransactionUiModel> {
        val q = category?.trim().orEmpty()
        return rows.filter { r -> r.txType == "expense" && (q.isEmpty() || sameName(r.category, q) || sameName(r.subcategory, q)) }
    }

    private fun sameName(name: String?, query: String): Boolean =
        name != null && (name.contains(query, ignoreCase = true) || query.contains(name, ignoreCase = true))

    fun totals(rows: List<TransactionUiModel>, currencyOf: (TransactionUiModel) -> String): Map<String, Double> =
        rows.groupBy(currencyOf).mapValues { (_, list) -> list.sumOf { it.amount } }

    // Per currency, the categories from the largest spend down.
    fun byCategory(rows: List<TransactionUiModel>, currencyOf: (TransactionUiModel) -> String): Map<String, List<Pair<String, Double>>> =
        rows.groupBy(currencyOf).mapValues { (_, list) ->
            list.groupBy { it.category }.map { (c, l) -> c to l.sumOf { it.amount } }.sortedByDescending { it.second }
        }

    fun largest(rows: List<TransactionUiModel>, currencyOf: (TransactionUiModel) -> String, limit: Int): Map<String, List<TransactionUiModel>> =
        rows.groupBy(currencyOf).mapValues { (_, list) -> list.sortedByDescending { it.amount }.take(limit) }

    fun describe(totals: Map<String, Double>): String =
        totals.entries.joinToString(" and ") { formatMoney(it.key, it.value) }.ifEmpty { "nothing" }
}
