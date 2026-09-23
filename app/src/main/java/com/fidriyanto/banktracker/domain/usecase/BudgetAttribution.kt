package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.domain.model.BudgetLine
import com.fidriyanto.banktracker.domain.model.TransactionUiModel

// Attributes cycle spend to budget lines, mirroring web/src/components/Budget.jsx: one transaction
// counts toward at most one line, lines tested in sort order (first match wins), amount converted
// when the line's currency differs from the wallet's.
object BudgetAttribution {
    private const val THB_WALLET = "BBL"

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

    // ac: android-budget-daily-allowance — spend attributed from the stored budget_lines match rules
    // Returns spent per line id (in each line's own currency).
    fun spendByLine(
        rows: List<TransactionUiModel>,
        lines: List<BudgetLine>,
        idrPerThb: Double,
    ): Map<Long, Double> {
        val matchers = compile(lines.sortedBy { it.sortOrder })
        val out = HashMap<Long, Double>()
        for (row in rows) {
            if (row.txType != "expense") continue
            val rowIsThb = row.wallet == THB_WALLET
            val hit = matchers.firstOrNull { matches(it, row) } ?: continue
            val line = hit.line
            var amount = row.amount
            if (line.currency == "THB" && !rowIsThb) amount = row.amount / idrPerThb
            if (line.currency == "IDR" && rowIsThb) amount = row.amount * idrPerThb
            out[line.id] = (out[line.id] ?: 0.0) + amount
        }
        return out
    }
}
