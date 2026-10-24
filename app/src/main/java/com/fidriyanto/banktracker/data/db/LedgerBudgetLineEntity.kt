package com.fidriyanto.banktracker.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

// ac: offline-mode - a budget line in offline mode, with the fields of the server's budget_lines table.
@Entity(tableName = "ledger_budget_lines")
data class LedgerBudgetLineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val kind: String,
    val currency: String,
    val target: Double,
    val matchWallets: List<String>? = null,
    val matchPattern: String? = null,
    val matchCategories: List<String>? = null,
    val sortOrder: Int = 0,
    val active: Boolean = true,
    val category: String? = null,
    val subcategory: String? = null,
    val dueDay: Int? = null,
)
