package com.fidriyanto.banktracker.domain.model

data class BudgetLine(
    val id: Long,
    val name: String,
    val kind: String,          // "fixed" | "flex"
    val currency: String,      // "THB" | "IDR"
    val target: Double,
    val matchWallets: List<String>?,
    val matchPattern: String?,
    val matchCategories: List<String>?,
    val sortOrder: Int,
)

data class BudgetLineSpend(
    val line: BudgetLine,
    val spent: Double,
) {
    val remaining: Double get() = (line.target - spent).coerceAtLeast(0.0)
    val progress: Float get() = if (line.target > 0) (spent / line.target).toFloat().coerceIn(0f, 1f) else 0f
    val isOver: Boolean get() = line.target > 0 && spent > line.target
}
