package com.fidriyanto.banktracker.data.datasource.remote.mapper

import com.fidriyanto.banktracker.data.datasource.remote.dto.BudgetDto
import com.fidriyanto.banktracker.data.db.MonthlyBudgetEntity

fun BudgetDto.toEntity() = MonthlyBudgetEntity(
    currency = currency, bills = bills, subscriptions = subscriptions,
    entertainment = entertainment, foodDrink = foodDrink, groceries = groceries,
    healthWellbeing = healthWellbeing, other = other, shopping = shopping,
    transport = transport, travel = travel, business = business, gifts = gifts
)

fun MonthlyBudgetEntity.toDto() = BudgetDto(
    currency = currency, bills = bills, subscriptions = subscriptions,
    entertainment = entertainment, foodDrink = foodDrink, groceries = groceries,
    healthWellbeing = healthWellbeing, other = other, shopping = shopping,
    transport = transport, travel = travel, business = business, gifts = gifts
)
