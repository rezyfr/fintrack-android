package com.fidriyanto.banktracker.data.repository

import com.fidriyanto.banktracker.data.datasource.WalletBalanceDataSource
import com.fidriyanto.banktracker.data.datasource.remote.dto.WalletBalanceAtDto
import com.fidriyanto.banktracker.data.datasource.remote.dto.WalletBalanceDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WalletBalanceRepositoryImpl @Inject constructor(
    private val dataSource: WalletBalanceDataSource,
) : WalletBalanceRepository {
    // ac: view-wallet-balances — balances are fetched from the wallet balances endpoint
    override suspend fun getBalances(): Result<List<WalletBalanceDto>> = runCatching {
        dataSource.getBalances()
    }

    // ac: account-growth-chart — balances on given dates, from the wallet balances history endpoint
    override suspend fun getBalancesAt(dates: List<String>): Result<List<WalletBalanceAtDto>> = runCatching {
        dataSource.getBalancesAt(dates)
    }
}
