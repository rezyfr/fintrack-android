package com.fidriyanto.banktracker.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "monthly_budget")
data class MonthlyBudgetEntity(
    @PrimaryKey val currency: String,
    val bills: Double,
    val subscriptions: Double,
    val entertainment: Double,
    val foodDrink: Double,
    val groceries: Double,
    val healthWellbeing: Double,
    val other: Double,
    val shopping: Double,
    val transport: Double,
    val travel: Double,
    val business: Double,
    val gifts: Double
)
