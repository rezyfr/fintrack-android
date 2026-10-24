package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.domain.model.CategoryRow
import com.fidriyanto.banktracker.domain.model.CurrencySummary
import com.fidriyanto.banktracker.domain.model.MonthTrendPoint
import com.fidriyanto.banktracker.domain.model.RateTable
import com.fidriyanto.banktracker.domain.model.TransactionUiModel
import com.fidriyanto.banktracker.domain.model.WalletRow
import java.time.LocalDate

// ac: insights-subcategory-breakdown — group a period's expense transactions by category, then by
// subcategory within each category. Unset subcategories are grouped under "None". A category is
// dropped only when none of its rows has a subcategory.
internal fun buildSubcategoryBreakdown(transactions: List<TransactionUiModel>): Map<String, List<CategoryRow>> {
    return transactions.filter { it.txType == "expense" && it.amount > 0.0 }
        .groupBy { it.category }
        .mapValues { (_, txs) ->
            val total = txs.sumOf { it.amount }
            txs.groupBy { it.subcategory?.takeIf { s -> s.isNotBlank() } ?: "None" }
                .map { (sub, group) ->
                    val amt = group.sumOf { it.amount }
                    CategoryRow(sub, amt, if (total > 0.0) (amt / total).toFloat() else 0f)
                }
                .sortedByDescending { it.amount }
        }
        .filterValues { groups -> groups.any { it.category != "None" } }
}

// ac: insights-pay-cycle-periods — build a currency's summary directly from the pay cycle's raw
// transactions (income/expense/category/subcategory), replacing the calendar-month
// monthly_overview aggregate so the numbers match the day-precise pay-cycle range.
// flow is the zone's money in and out. The transactions only feed the breakdowns.
// previous holds the previous period's rows for the same zone. It is null when they were not loaded.
internal fun buildSummaryFromTransactions(
    transactions: List<TransactionUiModel>,
    flow: MoneyFlow,
    previous: List<TransactionUiModel>? = null,
): CurrencySummary {
    val expenseRows = transactions.filter { it.txType == "expense" }
    val expenses = expenseRows.sumOf { it.amount }
    // ac: insights-category-vs-previous — each category row carries the previous period's spend
    val previousByCategory = previous
        ?.filter { it.txType == "expense" }
        ?.groupBy { it.category }
        ?.mapValues { (_, txs) -> txs.sumOf { it.amount } }
    val categoryBreakdown = expenseRows
        .groupBy { it.category }
        .mapValues { (_, txs) -> txs.sumOf { it.amount } }
        .filter { it.value > 0.0 }
        .toList()
        .sortedByDescending { it.second }
        .map { (cat, amt) ->
            CategoryRow(
                cat, amt, if (expenses > 0.0) (amt / expenses).toFloat() else 0f,
                previousAmount = previousByCategory?.let { it[cat] ?: 0.0 },
            )
        }
    return CurrencySummary(
        moneyIn = flow.moneyIn, moneyOut = flow.moneyOut, net = flow.net,
        categorySpend = expenses,
        categoryBreakdown = categoryBreakdown,
        subcategoryBreakdown = buildSubcategoryBreakdown(transactions),
        walletBreakdown = buildWalletBreakdown(expenseRows, expenses),
        topExpenses = expenseRows.filter { it.amount > 0.0 }.sortedByDescending { it.amount }.take(TOP_EXPENSES),
        dailySpend = expenseRows.groupBy { it.dateIso.take(10) }.mapValues { (_, txs) -> txs.sumOf { it.amount } },
    )
}

private const val TOP_EXPENSES = 5

// ac: insights-paid-with — expense total per wallet with its share of the period's expenses
private fun buildWalletBreakdown(expenseRows: List<TransactionUiModel>, expenses: Double): List<WalletRow> =
    expenseRows
        .filter { it.wallet != null && it.amount > 0.0 }
        .groupBy { it.wallet!! }
        .map { (wallet, txs) ->
            val amt = txs.sumOf { it.amount }
            WalletRow(wallet, amt, if (expenses > 0.0) (amt / expenses).toFloat() else 0f)
        }
        .sortedByDescending { it.amount }

// ac: insights-six-month-trend — money in and out of [zone] for each cycle. rowsByCycle holds every
// row of each cycle, because a transfer between two zones counts in both.
internal fun buildMonthlyTrend(
    cycles: List<PayCycle.Range>,
    rowsByCycle: List<List<TransactionUiModel>>,
    zone: String,
    zoneOf: (String?) -> String,
    today: LocalDate = LocalDate.now(),
): List<MonthTrendPoint> = cycles.zip(rowsByCycle) { cycle, rows ->
    val flow = moneyFlow(rows, zone, zoneOf)
    MonthTrendPoint(cycle.toIso, flow.moneyIn, flow.moneyOut, inProgress = !today.isBefore(cycle.start) && !today.isAfter(cycle.end))
}

// ac: insights-combined-currency - each row in the home currency. An amount is multiplied by the rate of
// its wallet's currency. A transfer converts its received amount with the destination wallet's rate.
internal fun convertToHome(
    rows: List<TransactionUiModel>,
    rates: RateTable,
    currencyOf: (String?) -> String,
): List<TransactionUiModel> = rows.map { r ->
    r.copy(
        amount = rates.toHome(currencyOf(r.wallet), r.amount),
        toAmount = r.toAmount?.let { rates.toHome(currencyOf(r.toWallet), it) },
    )
}
