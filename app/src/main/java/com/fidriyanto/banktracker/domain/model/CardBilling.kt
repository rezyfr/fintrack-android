package com.fidriyanto.banktracker.domain.model

data class CardBilling(
    val wallet: String,
    val cutoffDay: Int,
    val dueDay: Int,
    val minPercent: Double,
    val minFullInstallments: Boolean,
)

data class CardStatement(
    val wallet: String,
    val cutoffIso: String,
    val dueIso: String,
    val balance: Double,
    val installment: Double,
    val minimum: Double,
    val daysUntilDue: Int,
)
