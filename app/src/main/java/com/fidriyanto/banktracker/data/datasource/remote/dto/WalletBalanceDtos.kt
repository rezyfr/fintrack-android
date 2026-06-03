package com.fidriyanto.banktracker.data.datasource.remote.dto

import com.google.gson.annotations.SerializedName

data class WalletBalanceDto(
    @SerializedName("id")       val id: String,
    @SerializedName("name")     val name: String,
    @SerializedName("currency") val currency: String,
    @SerializedName("type")     val type: String,
    @SerializedName("balance")  val balance: Double,
)

data class ReconciliationRequest(
    @SerializedName("p_month") val pMonth: String,
)

data class ReconciliationRowDto(
    @SerializedName("wallet_id")          val walletId: String,
    @SerializedName("wallet_name")        val walletName: String,
    @SerializedName("currency")           val currency: String,
    @SerializedName("wallet_type")        val walletType: String,
    @SerializedName("opening_balance")    val openingBalance: Double?,
    @SerializedName("net_change")         val netChange: Double,
    @SerializedName("calculated_closing") val calculatedClosing: Double?,
    @SerializedName("next_opening")       val nextOpening: Double?,
    @SerializedName("difference")         val difference: Double?,
)

data class StatementBalanceUpsertDto(
    @SerializedName("wallet_id")       val walletId: String,
    @SerializedName("month")           val month: String,
    @SerializedName("opening_balance") val openingBalance: Double,
)
