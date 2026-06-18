package com.fidriyanto.banktracker.data.datasource.remote.dto

import com.google.gson.annotations.SerializedName

data class InstallmentDto(
    @SerializedName("id")                  val id: Long,
    @SerializedName("merchant")            val merchant: String,
    @SerializedName("merchant_pattern")    val merchantPattern: String,
    @SerializedName("wallet")              val wallet: String,
    @SerializedName("installment_amount")  val installmentAmount: Double,
    @SerializedName("total_installments")  val totalInstallments: Int,
    @SerializedName("current_step")        val currentStep: Int,
    @SerializedName("due_day")             val dueDay: Int,
    @SerializedName("status")             val status: String,
    @SerializedName("excluded")           val excluded: Boolean = false,
)
