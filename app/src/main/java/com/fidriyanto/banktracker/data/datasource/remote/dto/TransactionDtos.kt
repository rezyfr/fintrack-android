package com.fidriyanto.banktracker.data.datasource.remote.dto

import com.google.gson.annotations.SerializedName

data class TransactionDto(
    @SerializedName("id")        val id: Long,
    @SerializedName("merchant")  val merchant: String?,
    @SerializedName("item")      val item: String?,
    @SerializedName("amount")    val amount: Double,
    @SerializedName("category")  val category: String?,
    @SerializedName("date")      val date: String?,
    @SerializedName("tab")       val tab: String?,
    @SerializedName("wallet")    val wallet: String?,
    @SerializedName("tx_type")   val txType: String?,
    @SerializedName("to_wallet") val toWallet: String?
)

data class TransactionInsertDto(
    @SerializedName("tab")       val tab: String,
    @SerializedName("date")      val date: String,
    @SerializedName("merchant")  val merchant: String,
    @SerializedName("item")      val item: String,
    @SerializedName("amount")    val amount: Double,
    @SerializedName("category")  val category: String,
    @SerializedName("note")      val note: String?,
    @SerializedName("wallet")    val wallet: String?,
    @SerializedName("tx_type")   val txType: String,
    @SerializedName("to_wallet") val toWallet: String?
)

data class TransactionPatchDto(
    @SerializedName("amount")    val amount: Double,
    @SerializedName("item")      val item: String,
    @SerializedName("category")  val category: String,
    @SerializedName("date")      val date: String,
    @SerializedName("wallet")    val wallet: String?,
    @SerializedName("tx_type")   val txType: String,
    @SerializedName("to_wallet") val toWallet: String?
)
