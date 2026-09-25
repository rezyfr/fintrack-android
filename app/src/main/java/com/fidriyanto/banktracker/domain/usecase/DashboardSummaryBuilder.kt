package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.domain.model.CategoryRow
import com.fidriyanto.banktracker.domain.model.CurrencySummary
import com.fidriyanto.banktracker.domain.model.MerchantRow
import com.fidriyanto.banktracker.domain.model.MerchantTotal
import com.fidriyanto.banktracker.domain.model.MonthlyOverviewSummary
import com.fidriyanto.banktracker.domain.model.TransactionUiModel

// ac: insights-subcategory-breakdown — group a period's expense transactions by category, then by
// subcategory within each category. Unset subcategories are grouped under "None". Only categories
// with more than one distinct subcategory group are kept, since a single group adds no detail.
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
        .filterValues { it.size > 1 }
}

internal fun buildTransportBreakdown(merchants: List<MerchantTotal>, topN: Int = 3): List<MerchantRow> {
    val total = merchants.sumOf { it.amount }
    if (total == 0.0) return emptyList()
    // ac: transport-provider-breakdown: top 3 merchants by spend, remaining grouped into Other
    val top = merchants.take(topN)
    val otherAmt = merchants.drop(topN).sumOf { it.amount }
    // ac: transport-provider-breakdown: each row has amount and percentage bar relative to total transport
    val rows = top.map { MerchantRow(it.merchant, it.amount, (it.amount / total).toFloat()) }
    return if (otherAmt > 0.0) rows + MerchantRow("Other", otherAmt, (otherAmt / total).toFloat()) else rows
}

internal fun buildSummary(rows: List<MonthlyOverviewSummary>, merchants: List<MerchantTotal> = emptyList()): CurrencySummary {
    val income = rows.sumOf { it.income }
    val expenses = rows.sumOf { it.totalExpenditure }
    val cats = listOf(
        "Bills" to rows.sumOf { it.bills }, "Subscriptions" to rows.sumOf { it.subscriptions },
        "Entertainment" to rows.sumOf { it.entertainment }, "Food & Drink" to rows.sumOf { it.foodDrink },
        "Groceries" to rows.sumOf { it.groceries }, "Health & Wellbeing" to rows.sumOf { it.healthWellbeing },
        "Family" to rows.sumOf { it.family }, "Other" to rows.sumOf { it.other },
        "Shopping" to rows.sumOf { it.shopping }, "Transport" to rows.sumOf { it.transport },
        "Travel" to rows.sumOf { it.travel }, "Business" to rows.sumOf { it.business },
        "Gifts" to rows.sumOf { it.gifts },
    ).filter { it.second > 0.0 }.sortedByDescending { it.second }
     .map { (cat, amt) -> CategoryRow(cat, amt, if (expenses > 0.0) (amt / expenses).toFloat() else 0f) }
    return CurrencySummary(income, expenses, income - expenses, cats, buildTransportBreakdown(merchants))
}
