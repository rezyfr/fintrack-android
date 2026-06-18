package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.repository.InstallmentRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateInstallmentNameUseCase @Inject constructor(
    private val repository: InstallmentRepository,
) {
    // ac: installment-name-edit
    suspend operator fun invoke(id: Long, merchant: String): Result<Unit> =
        repository.updateMerchant(id, merchant)
}
