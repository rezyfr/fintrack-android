package com.fidriyanto.banktracker.data.repository

import com.fidriyanto.banktracker.data.datasource.remote.dto.WalletBalanceAtDto
import com.fidriyanto.banktracker.data.datasource.remote.dto.WalletBalanceDto

interface WalletBalanceRepository {
    suspend fun getBalances(): Result<List<WalletBalanceDto>>
    suspend fun getBalancesAt(dates: List<String>): Result<List<WalletBalanceAtDto>>
}
