package com.fidriyanto.banktracker.data.repository

import com.fidriyanto.banktracker.data.datasource.WalletBalanceDataSource
import com.fidriyanto.banktracker.data.datasource.remote.dto.ReconciliationRowDto
import com.fidriyanto.banktracker.data.datasource.remote.dto.WalletBalanceDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WalletBalanceRepositoryImpl @Inject constructor(
    private val dataSource: WalletBalanceDataSource,
) : WalletBalanceRepository {

    // ac: view-wallet-balances — balances are fetched from the wallet_balances Supabase view
    override suspend fun getBalances(): Result<List<WalletBalanceDto>> = runCatching {
        dataSource.getBalances()
    }

    // ac: reconcile-wallet-monthly — fetches reconciliation data for the selected month
    override suspend fun getReconciliation(month: String): Result<List<ReconciliationRowDto>> = runCatching {
        dataSource.getReconciliation(month)
    }

    // ac: reconcile-wallet-monthly — saving an opening balance persists to statement_balances
    override suspend fun upsertStatementBalance(walletId: String, month: String, openingBalance: Double): Result<Unit> = runCatching {
        dataSource.upsertStatementBalance(walletId, month, openingBalance)
    }
}
