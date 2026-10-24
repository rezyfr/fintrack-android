package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.repository.InstallmentRepository
import com.fidriyanto.banktracker.domain.model.Installment
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IncrementInstallmentStepUseCase @Inject constructor(
    private val repository: InstallmentRepository,
) {
    // ac: ac-isc-1, ac-isc-2
    suspend operator fun invoke(installment: Installment): Result<Unit> {
        val newStep = installment.currentStep + 1
        val newStatus = if (newStep >= installment.totalInstallments) "completed" else installment.status
        return repository.incrementStep(installment.id, newStep, newStatus)
    }
}
