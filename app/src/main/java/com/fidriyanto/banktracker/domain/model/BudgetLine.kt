package com.fidriyanto.banktracker.domain.model

data class BudgetLine(
    val id: Long,
    val name: String,
    val kind: String, // "fixed" | "flex"
    val currency: String, // an ISO currency code
    val target: Double,
    val matchWallets: List<String>?,
    val matchPattern: String?,
    val matchCategories: List<String>?,
    val sortOrder: Int,
    // ac: budget-by-category — a line set up by category counts the expenses of that category, and of
    // the subcategory when one is set, in the wallets of its currency. A line without a category
    // still uses the match rules above.
    val category: String? = null,
    val subcategory: String? = null,
    // The day of the month a fixed line is due, or null
    val dueDay: Int? = null,
)

data class BudgetLineSpend(
    val line: BudgetLine,
    val spent: Double,
    // ac: budget-line-details-and-assignment — the line's most recent attributed transactions (up to 3)
    val recent: List<TransactionUiModel> = emptyList(),
) {
    val remaining: Double get() = (line.target - spent).coerceAtLeast(0.0)
    val progress: Float get() = if (line.target > 0) (spent / line.target).toFloat().coerceIn(0f, 1f) else 0f
    val isOver: Boolean get() = line.target > 0 && spent > line.target
}
