package com.fidriyanto.banktracker.domain.model

// What the server needs to create an installment plan. It cannot be edited after it is created.
data class NewInstallment(
    val merchant: String,
    val merchantPattern: String,
    val wallet: String,
    val installmentAmount: Double,
    val totalInstallments: Int,
    val currentStep: Int,
    val dueDay: Int,
    val status: String,
)
