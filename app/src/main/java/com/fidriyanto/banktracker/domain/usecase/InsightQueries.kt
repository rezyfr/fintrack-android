package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.domain.model.BudgetLineSpend
import com.fidriyanto.banktracker.domain.model.RateTable
import com.fidriyanto.banktracker.domain.model.TransactionUiModel

// One name and what was spent on it.
data class Merchant(val name: String, val currency: String, val total: Double, val times: Int)

// A payment that repeats about every month.
data class RecurringPayment(val name: String, val currency: String, val amount: Double, val day: Int, val months: Int, val lastDate: String)

data class CashFlow(val income: Double, val expense: Double) {
    val net: Double get() = income - expense
}

// Plain counting for the Ask tools. The model asks, and these functions count, so the numbers are exact.
object InsightQueries {
    private const val MIN_MONTHS = 3
    private const val AMOUNT_SHARE = 0.6
    private const val DAY_SHARE = 0.8

    // The budget line a name points to: its name, its category or its subcategory, in either direction.
    fun budgetLine(lines: List<BudgetLineSpend>, query: String): BudgetLineSpend? =
        lines.firstOrNull { s -> listOfNotNull(s.line.name, s.line.category, s.line.subcategory).any { it.contains(query, true) || query.contains(it, true) } }

    fun income(rows: List<TransactionUiModel>, text: String?): List<TransactionUiModel> {
        val q = text?.trim().orEmpty()
        return rows.filter { it.txType == "income" && (q.isEmpty() || it.item.contains(q, ignoreCase = true)) }
    }

    // Money in and money out per currency. Transfers and investments are neither.
    fun flow(rows: List<TransactionUiModel>, currencyOf: (TransactionUiModel) -> String): Map<String, CashFlow> =
        rows.filter { it.txType == "income" || it.txType == "expense" }.groupBy(currencyOf).mapValues { (_, list) ->
            CashFlow(list.filter { it.txType == "income" }.sumOf { it.amount }, list.filter { it.txType == "expense" }.sumOf { it.amount })
        }

    // The names with the most money first, across currencies. A name in two currencies counts as two. The order
    // compares the amounts in the home currency, and each amount keeps its own currency.
    fun merchants(rows: List<TransactionUiModel>, currencyOf: (TransactionUiModel) -> String, rates: RateTable, limit: Int): List<Merchant> =
        rows.groupBy { currencyOf(it) to it.item.trim().lowercase() }
            .map { (key, l) -> Merchant(l.first().item.trim(), key.first, l.sumOf { it.amount }, l.size) }
            .sortedByDescending { rates.toHome(it.currency, it.total) }.take(limit)

    // Per currency, the total of each month, oldest first. A month is yyyy-MM.
    fun monthly(rows: List<TransactionUiModel>, currencyOf: (TransactionUiModel) -> String): Map<String, Map<String, Double>> =
        rows.groupBy(currencyOf).mapValues { (_, list) ->
            list.groupBy { it.dateIso.take(7) }.mapValues { (_, l) -> l.sumOf { it.amount } }.toSortedMap()
        }

    // Payments that come back each month: the same name with one usual amount, or the same amount on the same day.
    fun recurring(rows: List<TransactionUiModel>, currencyOf: (TransactionUiModel) -> String, limit: Int): List<RecurringPayment> {
        val expenses = rows.filter { it.txType == "expense" }
        val byName = expenses.groupBy { currencyOf(it) to it.item.trim().lowercase() }.mapNotNull { (key, list) ->
            val months = list.map { it.dateIso.take(7) }.toSet().size
            val top = list.groupBy { it.amount }.maxByOrNull { it.value.size } ?: return@mapNotNull null
            if (months < MIN_MONTHS || top.value.size.toDouble() / list.size < AMOUNT_SHARE) return@mapNotNull null
            RecurringPayment(list.first().item.trim(), key.first, top.key, usualDay(top.value), months, list.maxOf { it.dateIso.take(10) })
        }
        val named = byName.map { it.currency to it.name.lowercase() }.toSet()
        val byAmount = expenses.filter { currencyOf(it) to it.item.trim().lowercase() !in named }.groupBy { Triple(currencyOf(it), it.wallet, it.amount) }.mapNotNull { (key, list) ->
            val months = list.map { it.dateIso.take(7) }.toSet().size
            val days = list.map { dayOf(it) }
            val top = days.groupBy { it }.maxByOrNull { it.value.size } ?: return@mapNotNull null
            if (months < MIN_MONTHS || top.value.size.toDouble() / list.size < DAY_SHARE) return@mapNotNull null
            RecurringPayment(list.first().item.trim(), key.first, key.third, top.key, months, list.maxOf { it.dateIso.take(10) })
        }
        return (byName + byAmount).sortedWith(compareByDescending<RecurringPayment> { it.months }.thenByDescending { it.amount }).take(limit)
    }

    private fun dayOf(r: TransactionUiModel): Int = r.dateIso.drop(8).take(2).toIntOrNull() ?: 0

    private fun usualDay(list: List<TransactionUiModel>): Int = list.map { dayOf(it) }.groupBy { it }.maxByOrNull { it.value.size }?.key ?: 0
}
