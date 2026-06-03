package com.fidriyanto.banktracker.data.repository

import com.fidriyanto.banktracker.data.datasource.remote.dto.ReconciliationRowDto
import com.fidriyanto.banktracker.data.datasource.remote.dto.WalletBalanceDto

interface WalletBalanceRepository {
    suspend fun getBalances(): Result<List<WalletBalanceDto>>
    suspend fun getReconciliation(month: String): Result<List<ReconciliationRowDto>>
    suspend fun upsertStatementBalance(walletId: String, month: String, openingBalance: Double): Result<Unit>
}
