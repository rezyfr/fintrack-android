package com.fidriyanto.banktracker.data.datasource.offline

import com.fidriyanto.banktracker.data.db.WalletNet

// ac: offline-mode - the balance of each account, as the server's wallet_balances view computes it: the
// opening balance, plus income, minus expenses and the transfers it sent, plus the transfers it received.
fun ledgerBalances(openingBalances: Map<String, Double>, ownFlow: List<WalletNet>, incoming: List<WalletNet>): Map<String, Double> {
    val own = ownFlow.mapNotNull { n -> n.walletId?.let { it to n.net } }.toMap()
    val received = incoming.mapNotNull { n -> n.walletId?.let { it to n.net } }.toMap()
    return openingBalances.mapValues { (id, opening) -> opening + (own[id] ?: 0.0) + (received[id] ?: 0.0) }
}
