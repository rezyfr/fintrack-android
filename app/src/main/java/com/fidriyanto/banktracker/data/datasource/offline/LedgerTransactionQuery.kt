package com.fidriyanto.banktracker.data.datasource.offline

// The SQL of a filtered transaction list and its arguments.
data class LedgerSql(val sql: String, val args: List<Any>)

// ac: offline-mode - builds the transaction list query. The filters work like GET /v1/transactions on the
// server: they combine with AND, a date range replaces the month, the search ignores case and treats % and _
// as plain text, and the newest transactions come first.
fun ledgerTransactionQuery(
    month: String?, wallet: String?, txType: String?, category: String?, search: String?,
    amountMin: Double?, amountMax: Double?, dateFrom: String?, dateTo: String?, involvingWallet: String?,
    limit: Int,
): LedgerSql {
    val where = mutableListOf<String>()
    val args = mutableListOf<Any>()
    fun add(clause: String, vararg values: Any) {
        where += clause
        args.addAll(values)
    }
    if (dateFrom != null || dateTo != null) {
        dateFrom?.let { add("date >= ?", it) }
        dateTo?.let { add("date <= ?", it) }
    } else if (month != null) {
        add("substr(date, 1, 7) = ?", month)
    }
    wallet?.let { add("wallet = ?", it) }
    involvingWallet?.let { add("(wallet = ? OR toWallet = ?)", it, it) }
    txType?.let { add("txType = ?", it) }
    category?.let { add("category = ?", it) }
    search?.takeIf { it.isNotEmpty() }?.let { add("item LIKE ? ESCAPE '\\'", "%" + escapeLike(it) + "%") }
    amountMin?.let { add("amount >= ?", it) }
    amountMax?.let { add("amount <= ?", it) }
    val clause = if (where.isEmpty()) "" else " WHERE " + where.joinToString(" AND ")
    return LedgerSql("SELECT * FROM ledger_transactions$clause ORDER BY date DESC, id DESC LIMIT ?", args + limit)
}

private fun escapeLike(text: String): String = text.replace(Regex("[\\\\%_]")) { "\\" + it.value }
