package com.fidriyanto.banktracker.data.datasource.remote.dto

import com.google.gson.annotations.SerializedName

data class TransactionDto(
    @SerializedName("id")        val id: Long,
    @SerializedName("item")      val item: String?,
    @SerializedName("amount")    val amount: Double,
    @SerializedName("category")  val category: String?,
    @SerializedName("date")      val date: String?,
    @SerializedName("tab")       val tab: String?,
    @SerializedName("wallet")    val wallet: String?,
    @SerializedName("tx_type")   val txType: String?,
    @SerializedName("to_wallet") val toWallet: String?,
    // ac: add-transfer-target-amount — received amount for cross-currency transfers, read back
    @SerializedName("to_amount") val toAmount: Double? = null,
    // ac: add-transaction-subcategory — optional subcategory read back from Supabase
    @SerializedName("subcategory") val subcategory: String? = null
)

data class TransactionInsertDto(
    @SerializedName("tab")       val tab: String,
    @SerializedName("date")      val date: String,
    @SerializedName("item")      val item: String,
    @SerializedName("amount")    val amount: Double,
    @SerializedName("category")  val category: String,
    @SerializedName("note")      val note: String?,
    @SerializedName("wallet")    val wallet: String?,
    @SerializedName("tx_type")   val txType: String,
    @SerializedName("to_wallet") val toWallet: String?,
    // ac: add-transfer-target-amount — persist the received amount for cross-currency transfers
    @SerializedName("to_amount") val toAmount: Double? = null,
    @SerializedName("subcategory") val subcategory: String? = null
)

data class TransactionPatchDto(
    @SerializedName("amount")    val amount: Double,
    @SerializedName("item")      val item: String,
    @SerializedName("category")  val category: String,
    @SerializedName("date")      val date: String,
    @SerializedName("wallet")    val wallet: String?,
    @SerializedName("tx_type")   val txType: String,
    @SerializedName("to_wallet") val toWallet: String?,
    @SerializedName("to_amount") val toAmount: Double?,
    @SerializedName("subcategory") val subcategory: String? = null,
)

// ac: batch-edit-transaction-category — partial patch so a batch category update never touches other fields
data class CategoryPatchDto(
    @SerializedName("category") val category: String,
)
