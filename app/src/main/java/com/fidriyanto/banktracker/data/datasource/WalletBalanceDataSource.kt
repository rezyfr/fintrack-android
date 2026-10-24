package com.fidriyanto.banktracker.data.datasource

import com.fidriyanto.banktracker.data.datasource.remote.dto.WalletBalanceAtDto
import com.fidriyanto.banktracker.data.datasource.remote.dto.WalletBalanceDto

interface WalletBalanceDataSource {
    suspend fun getBalances(): List<WalletBalanceDto>
    suspend fun getBalancesAt(dates: List<String>): List<WalletBalanceAtDto>
}
