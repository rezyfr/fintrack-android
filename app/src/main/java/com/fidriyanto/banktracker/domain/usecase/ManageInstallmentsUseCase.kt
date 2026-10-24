package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.repository.InstallmentRepository
import com.fidriyanto.banktracker.domain.model.Installment
import com.fidriyanto.banktracker.domain.model.NewInstallment
import javax.inject.Inject

// ac: installment-plan-add, installment-plan-delete - create and delete an installment plan.
// The server cannot edit the amount, the due day, the total steps, the account or the statement text.
class ManageInstallmentsUseCase @Inject constructor(
    private val repository: InstallmentRepository,
) {
    // A plan with every step paid is created as completed. Any other plan is active.
    suspend fun create(plan: NewInstallment): Result<Installment> {
        val status = if (plan.currentStep >= plan.totalInstallments) "completed" else "active"
        return repository.create(plan.copy(status = status))
    }

    suspend fun delete(id: Long): Result<Unit> = repository.delete(id)
}
