package com.fidriyanto.banktracker.data.datasource.remote.dto

import com.google.gson.annotations.SerializedName

data class AccountDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("currency") val currency: String,
    @SerializedName("sort_order") val sortOrder: Int = 0,
    @SerializedName("is_default") val isDefault: Boolean = false,
    @SerializedName("type") val type: String = "checking",
    @SerializedName("opening_balance") val openingBalance: Double = 0.0,
)
