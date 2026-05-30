package com.fidriyanto.banktracker.data.datasource.remote.mapper

import com.fidriyanto.banktracker.data.datasource.remote.dto.MonthlyOverviewRowDto
import com.fidriyanto.banktracker.data.db.MonthlyOverviewEntity

fun MonthlyOverviewRowDto.toEntity() = MonthlyOverviewEntity(
    month            = month,
    currency         = currency,
    bills            = bills,
    subscriptions    = subscriptions,
    entertainment    = entertainment,
    foodDrink        = foodDrink,
    groceries        = groceries,
    healthWellbeing  = healthWellbeing,
    family           = family,
    other            = other,
    shopping         = shopping,
    transport        = transport,
    travel           = travel,
    business         = business,
    gifts            = gifts,
    totalExpenditure = totalExpenditure,
    income           = income,
    grossSavings     = grossSavings
)
