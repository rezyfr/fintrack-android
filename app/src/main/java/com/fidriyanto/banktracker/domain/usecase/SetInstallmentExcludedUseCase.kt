package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.repository.InstallmentRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SetInstallmentExcludedUseCase @Inject constructor(
    private val repository: InstallmentRepository,
) {
    // ac: ac-isc-4
    suspend operator fun invoke(id: Long, excluded: Boolean): Result<Unit> =
        repository.setExcluded(id, excluded)
}
