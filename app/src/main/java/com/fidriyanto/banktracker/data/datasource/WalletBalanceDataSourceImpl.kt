package com.fidriyanto.banktracker.data.datasource

import com.fidriyanto.banktracker.data.datasource.remote.WalletBalanceApiService
import com.fidriyanto.banktracker.data.datasource.remote.dto.WalletBalanceAtDto
import com.fidriyanto.banktracker.data.datasource.remote.dto.WalletBalanceDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WalletBalanceDataSourceImpl @Inject constructor(
    private val service: WalletBalanceApiService,
) : WalletBalanceDataSource {
    override suspend fun getBalances(): List<WalletBalanceDto> = service.getWalletBalances()

    override suspend fun getBalancesAt(dates: List<String>): List<WalletBalanceAtDto> =
        service.getWalletBalancesAt(dates.joinToString(","))
}
