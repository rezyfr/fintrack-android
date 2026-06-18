package com.fidriyanto.banktracker.domain.model

import java.time.LocalDate

data class Installment(
    val id: Long,
    val merchant: String,
    val wallet: String,
    val installmentAmount: Double,
    val totalInstallments: Int,
    val currentStep: Int,
    val dueDay: Int,
    val status: String,
    val excluded: Boolean = false,
) {
    val remaining: Int get() = totalInstallments - currentStep
    val isCompleted: Boolean get() = status == "completed"
    val progress: Float get() = currentStep.toFloat() / totalInstallments.coerceAtLeast(1)

    fun nextDueDate(): LocalDate {
        val today = LocalDate.now()
        val thisMonth = today.withDayOfMonth(dueDay.coerceIn(1, today.lengthOfMonth()))
        return if (thisMonth.isAfter(today)) thisMonth else thisMonth.plusMonths(1)
    }

    fun estimatedFinish(): LocalDate = nextDueDate().plusMonths((remaining - 1L).coerceAtLeast(0))
}
