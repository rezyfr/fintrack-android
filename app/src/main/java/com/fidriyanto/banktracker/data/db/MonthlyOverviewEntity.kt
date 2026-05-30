package com.fidriyanto.banktracker.data.db

import androidx.room.Entity

@Entity(tableName = "monthly_overview", primaryKeys = ["month", "currency"])
data class MonthlyOverviewEntity(
    val month: String,           // "January 2026"
    val currency: String,        // "THB" or "IDR"
    val bills: Double,
    val subscriptions: Double,
    val entertainment: Double,
    val foodDrink: Double,
    val groceries: Double,
    val healthWellbeing: Double,
    val family: Double,
    val other: Double,
    val shopping: Double,
    val transport: Double,
    val travel: Double,
    val business: Double,
    val gifts: Double,
    val totalExpenditure: Double,
    val income: Double,
    val grossSavings: Double
)
