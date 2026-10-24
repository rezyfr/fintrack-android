package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.repository.InstallmentRepository
import com.fidriyanto.banktracker.domain.model.Installment
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetInstallmentsUseCase @Inject constructor(
    private val repository: InstallmentRepository,
) {
    // ac: installment-overview
    suspend operator fun invoke(): Result<List<Installment>> = repository.getAll()
}
