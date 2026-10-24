package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.domain.model.TransactionUiModel

// Money in and money out of one currency zone, shared by Home and Insights.
// Money in is income plus the received amount of a transfer that arrives from another zone. Money
// out is expense plus a transfer that leaves for another zone. A transfer inside one zone moves
// money between the user's own accounts, so it is in neither total.
data class MoneyFlow(val moneyIn: Double, val moneyOut: Double) {
    val net: Double get() = moneyIn - moneyOut
}

// zoneOf maps a wallet id to its zone: its currency code, or a separate zone such as investments.
internal fun moneyFlow(rows: List<TransactionUiModel>, zone: String, zoneOf: (String?) -> String): MoneyFlow {
    val walletRows = rows.filter { it.wallet != null }
    val moneyIn = walletRows.sumOf { r ->
        when {
            r.txType == "income" && zoneOf(r.wallet) == zone -> r.amount
            r.txType == "transfer" && r.toWallet != null && zoneOf(r.toWallet) == zone && zoneOf(r.wallet) != zone -> r.toAmount ?: r.amount
            else -> 0.0
        }
    }
    val moneyOut = walletRows.sumOf { r ->
        when {
            r.txType == "expense" && zoneOf(r.wallet) == zone -> r.amount
            r.txType == "transfer" && r.toWallet != null && zoneOf(r.wallet) == zone && zoneOf(r.toWallet) != zone -> r.amount
            else -> 0.0
        }
    }
    return MoneyFlow(moneyIn, moneyOut)
}
