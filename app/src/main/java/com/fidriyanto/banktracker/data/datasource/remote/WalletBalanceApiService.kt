package com.fidriyanto.banktracker.data.datasource.remote

import com.fidriyanto.banktracker.data.datasource.remote.dto.WalletBalanceAtDto
import com.fidriyanto.banktracker.data.datasource.remote.dto.WalletBalanceDto
import retrofit2.http.GET
import retrofit2.http.Query

interface WalletBalanceApiService {
    @GET("v1/wallet-balances")
    suspend fun getWalletBalances(): List<WalletBalanceDto>

    // ac: account-growth-chart — every account's balance on each of the given days, written as YYYY-MM-DD and separated by commas
    @GET("v1/wallet-balances/history")
    suspend fun getWalletBalancesAt(@Query("dates") dates: String): List<WalletBalanceAtDto>
}
