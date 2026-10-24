package com.fidriyanto.banktracker.data.datasource

import com.fidriyanto.banktracker.data.db.TransactionEntity

interface TransactionFetchDataSource {
    suspend fun fetch(
        month: String?, wallet: String?, txType: String?, category: String? = null,
        search: String? = null, amountMin: Double? = null, amountMax: Double? = null,
        dateFrom: String? = null, dateTo: String? = null,
        // ac: insights-wallet-flow — rows where this wallet is the source or the transfer target
        involvingWallet: String? = null,
    ): Result<List<TransactionEntity>>

    // ac: exchange-rate-from-transfers — received per sent amount of the latest [limit] transfers from
    // fromWallets to toWallets that carry a received amount, newest first
    suspend fun recentTransferRates(fromWallets: List<String>, toWallets: List<String>, limit: Int = 10): Result<List<Double>>
}
