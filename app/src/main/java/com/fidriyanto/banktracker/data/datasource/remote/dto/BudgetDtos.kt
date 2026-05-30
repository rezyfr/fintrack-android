package com.fidriyanto.banktracker.data.datasource.remote.dto

import com.google.gson.annotations.SerializedName

data class BudgetDto(
    @SerializedName("currency")         val currency: String,
    @SerializedName("bills")            val bills: Double,
    @SerializedName("subscriptions")    val subscriptions: Double,
    @SerializedName("entertainment")    val entertainment: Double,
    @SerializedName("food_drink")       val foodDrink: Double,
    @SerializedName("groceries")        val groceries: Double,
    @SerializedName("health_wellbeing") val healthWellbeing: Double,
    @SerializedName("other")            val other: Double,
    @SerializedName("shopping")         val shopping: Double,
    @SerializedName("transport")        val transport: Double,
    @SerializedName("travel")           val travel: Double,
    @SerializedName("business")         val business: Double,
    @SerializedName("gifts")            val gifts: Double
)
