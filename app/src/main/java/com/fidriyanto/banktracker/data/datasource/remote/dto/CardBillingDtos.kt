package com.fidriyanto.banktracker.data.datasource.remote.dto

import com.google.gson.annotations.SerializedName

data class CardBillingDto(
    @SerializedName("wallet")                val wallet: String,
    @SerializedName("cutoff_day")            val cutoffDay: Int,
    @SerializedName("due_day")               val dueDay: Int,
    @SerializedName("min_percent")           val minPercent: Double,
    @SerializedName("min_full_installments") val minFullInstallments: Boolean = false,
)
