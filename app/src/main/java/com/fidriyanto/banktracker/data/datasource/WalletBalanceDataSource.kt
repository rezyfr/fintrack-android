package com.fidriyanto.banktracker.data.datasource

import com.fidriyanto.banktracker.data.datasource.remote.dto.ReconciliationRowDto
import com.fidriyanto.banktracker.data.datasource.remote.dto.WalletBalanceDto

interface WalletBalanceDataSource {
    suspend fun getBalances(): List<WalletBalanceDto>
    suspend fun getReconciliation(month: String): List<ReconciliationRowDto>
    suspend fun upsertStatementBalance(walletId: String, month: String, openingBalance: Double)
}
