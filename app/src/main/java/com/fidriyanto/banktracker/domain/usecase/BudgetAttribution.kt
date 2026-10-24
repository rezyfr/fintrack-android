package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.domain.model.BudgetLine
import com.fidriyanto.banktracker.domain.model.RateTable
import com.fidriyanto.banktracker.domain.model.TransactionUiModel

// Attributes cycle spend to budget lines, the same rule as web/src/utils/budgetMatch.js: one
// transaction counts toward at most one line, chosen by lineFor. The amount is converted when the
// line's currency differs from the wallet's, which only happens on a line that uses match rules.
object BudgetAttribution {
    // Spend (in the line's own currency) and the transactions attributed to a line.
    data class LineAgg(val spent: Double, val rows: List<TransactionUiModel>)

    private data class Matcher(val line: BudgetLine, val regex: Regex?)

    private fun compile(lines: List<BudgetLine>): List<Matcher> = lines.map { l ->
        val re = l.matchPattern?.let { runCatching { Regex(it, RegexOption.IGNORE_CASE) }.getOrNull() }
        Matcher(l, re)
    }

    private fun matches(m: Matcher, row: TransactionUiModel): Boolean {
        val wallets = m.line.matchWallets
        if (wallets != null && row.wallet !in wallets) return false
        if (m.regex != null && m.regex.containsMatchIn(row.item)) return true
        val cats = m.line.matchCategories
        if (cats != null && row.category in cats) return true
        return false
    }

    // ac: configurable-currency-pair - a row's currency comes from its account. The amount is converted
    // to the line's currency with the rate table.
    fun amountInLine(row: TransactionUiModel, line: BudgetLine, rates: RateTable, currencyByWallet: Map<String, String>): Double =
        rates.convert(row.amount, currencyByWallet[row.wallet] ?: rates.homeCode, line.currency)

    // ac: budget-by-category — the line an expense counts toward, or null when it has none.
    // A line set up by category takes the expenses of that category in the wallets of its currency;
    // a line for the expense's subcategory wins over the line for the whole category. Lines without a
    // category keep the old rules: a manual assignment (budget_line_id), then the first matching rule.
    fun lineFor(
        row: TransactionUiModel,
        lines: List<BudgetLine>,
        currencyByWallet: Map<String, String>,
        homeCode: String,
    ): BudgetLine? {
        if (row.txType != "expense") return null
        val currency = currencyByWallet[row.wallet] ?: homeCode
        val byCategory = lines.filter { it.category != null && it.currency == currency && it.category == row.category }
        byCategory.firstOrNull { it.subcategory != null && it.subcategory == row.subcategory }?.let { return it }
        byCategory.firstOrNull { it.subcategory == null }?.let { return it }
        val ruleLines = lines.filter { it.category == null }
        ruleLines.firstOrNull { it.id == row.budgetLineId }?.let { return it }
        return compile(ruleLines.sortedBy { it.sortOrder }).firstOrNull { matches(it, row) }?.line
    }

    // Each line's spend (in the line's own currency) and its rows, newest first.
    fun attribute(
        rows: List<TransactionUiModel>,
        lines: List<BudgetLine>,
        rates: RateTable,
        currencyByWallet: Map<String, String> = emptyMap(),
    ): Map<Long, LineAgg> {
        val spent = HashMap<Long, Double>()
        val rowsByLine = HashMap<Long, MutableList<TransactionUiModel>>()
        for (row in rows) {
            val line = lineFor(row, lines, currencyByWallet, rates.homeCode) ?: continue
            spent[line.id] = (spent[line.id] ?: 0.0) + amountInLine(row, line, rates, currencyByWallet)
            rowsByLine.getOrPut(line.id) { mutableListOf() }.add(row)
        }
        return lines.associate { l ->
            l.id to LineAgg(spent[l.id] ?: 0.0, (rowsByLine[l.id] ?: emptyList()).sortedByDescending { it.dateIso })
        }
    }

    // ac: budget-unbudgeted-line — the expense rows that count toward no line
    fun unattributed(
        rows: List<TransactionUiModel>,
        lines: List<BudgetLine>,
        currencyByWallet: Map<String, String>,
        homeCode: String,
    ): List<TransactionUiModel> =
        rows.filter { it.txType == "expense" && lineFor(it, lines, currencyByWallet, homeCode) == null }
}
