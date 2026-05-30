package com.fidriyanto.banktracker.data.datasource.remote.dto

import com.google.gson.annotations.SerializedName

data class MonthlyOverviewRequest(
    @SerializedName("p_months") val pMonths: List<String>
)

data class MonthlyOverviewRowDto(
    @SerializedName("month")             val month: String,
    @SerializedName("currency")          val currency: String,
    @SerializedName("bills")             val bills: Double,
    @SerializedName("subscriptions")     val subscriptions: Double,
    @SerializedName("entertainment")     val entertainment: Double,
    @SerializedName("food_drink")        val foodDrink: Double,
    @SerializedName("groceries")         val groceries: Double,
    @SerializedName("health_wellbeing")  val healthWellbeing: Double,
    @SerializedName("family")            val family: Double,
    @SerializedName("other")             val other: Double,
    @SerializedName("shopping")          val shopping: Double,
    @SerializedName("transport")         val transport: Double,
    @SerializedName("travel")            val travel: Double,
    @SerializedName("business")          val business: Double,
    @SerializedName("gifts")             val gifts: Double,
    @SerializedName("total_expenditure") val totalExpenditure: Double,
    @SerializedName("income")            val income: Double,
    @SerializedName("gross_savings")     val grossSavings: Double
)
