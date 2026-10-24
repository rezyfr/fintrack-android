package com.fidriyanto.banktracker.data.datasource.remote.dto

import com.google.gson.annotations.SerializedName

data class WalletBalanceDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("currency") val currency: String,
    @SerializedName("type") val type: String,
    @SerializedName("balance") val balance: Double,
)

// ac: account-growth-chart — the balance of one account on one date, from the wallet_balances_at function
data class WalletBalanceAtDto(
    @SerializedName("wallet_id") val walletId: String,
    @SerializedName("as_of") val asOf: String,
    @SerializedName("balance") val balance: Double,
)
