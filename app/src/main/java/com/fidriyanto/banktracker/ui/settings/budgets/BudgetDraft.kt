package com.fidriyanto.banktracker.ui.settings.budgets

import com.fidriyanto.banktracker.domain.model.BudgetLine

enum class BudgetError { CATEGORY, TARGET }

const val KIND_FLEX = "flex"
const val KIND_FIXED = "fixed"

// What the editor holds while the user fills a budget line in. The target is text so a half-typed number is kept.
// A line that uses match rules keeps its text rule: `original` holds it, and only name, type, currency and target change.
data class BudgetDraft(
    val original: BudgetLine? = null,
    val kind: String = KIND_FLEX,
    val category: String = "",
    val subcategory: String? = null,
    val subOn: Boolean = false,
    val name: String = "",
    val target: String = "",
    val currency: String = "",
    val dueOn: Boolean = false,
    val dueDay: Int = 1,
) {
    val isNew: Boolean get() = original == null

    // A line that matches by text has no category.
    val byRules: Boolean get() = original != null && original.category == null

    val targetValue: Double? get() = target.replace(",", "").toDoubleOrNull()?.takeIf { it >= 0.0 }

    // The name is optional. A blank name falls back to the subcategory, then to the category.
    val displayName: String
        get() = name.trim().ifBlank {
            if (byRules) original?.name.orEmpty() else subcategory.takeIf { subOn }.orEmpty().ifBlank { category }
        }

    // Step 1: a line that follows a category needs one.
    fun whatError(): BudgetError? = if (!byRules && category.isBlank()) BudgetError.CATEGORY else null

    // Step 2: the target is a number of zero or more.
    fun howError(): BudgetError? = if (targetValue == null) BudgetError.TARGET else null

    // A new line has id 0. The use case sets its sort order.
    fun toBudgetLine(): BudgetLine {
        val base = original ?: BudgetLine(0L, "", kind, currency, 0.0, null, null, null, 0)
        return base.copy(
            name = displayName,
            kind = kind,
            currency = currency,
            target = targetValue ?: 0.0,
            category = if (byRules) null else category,
            subcategory = if (byRules) null else subcategory.takeIf { subOn },
            dueDay = dueDay.takeIf { kind == KIND_FIXED && dueOn },
        )
    }

    companion object {
        fun from(line: BudgetLine) = BudgetDraft(
            original = line, kind = line.kind, category = line.category.orEmpty(), subcategory = line.subcategory,
            subOn = line.subcategory != null, name = line.name, target = line.target.toString().removeSuffix(".0"),
            currency = line.currency, dueOn = line.dueDay != null, dueDay = line.dueDay ?: 1,
        )
    }
}
