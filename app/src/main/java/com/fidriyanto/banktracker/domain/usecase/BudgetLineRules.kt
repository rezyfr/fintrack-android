package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.domain.model.BudgetLine

// Rules for editing budget lines
object BudgetLineRules {
    // A new line goes after every line that exists.
    fun nextSortOrder(lines: List<BudgetLine>): Int = (lines.maxOfOrNull { it.sortOrder } ?: 0) + 1

    // Another line with the same category, subcategory and currency. BudgetAttribution.lineFor takes the
    // first one, so the other one never counts an expense. A line without a category has no twin.
    fun duplicateOf(line: BudgetLine, others: List<BudgetLine>): BudgetLine? {
        if (line.category == null) return null
        return others.firstOrNull {
            it.id != line.id && it.category == line.category && it.subcategory == line.subcategory && it.currency == line.currency
        }
    }
}
