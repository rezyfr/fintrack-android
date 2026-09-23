package com.fidriyanto.banktracker.data.datasource.remote.dto

import com.google.gson.annotations.SerializedName

data class BudgetLineDto(
    @SerializedName("id")               val id: Long,
    @SerializedName("name")             val name: String,
    @SerializedName("kind")             val kind: String,
    @SerializedName("currency")         val currency: String,
    @SerializedName("target")           val target: Double,
    @SerializedName("match_wallets")    val matchWallets: List<String>?,
    @SerializedName("match_pattern")    val matchPattern: String?,
    @SerializedName("match_categories") val matchCategories: List<String>?,
    @SerializedName("sort_order")       val sortOrder: Int,
    @SerializedName("active")           val active: Boolean = true,
)
