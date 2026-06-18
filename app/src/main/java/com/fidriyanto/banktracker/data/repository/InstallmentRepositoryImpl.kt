package com.fidriyanto.banktracker.data.repository

import com.fidriyanto.banktracker.data.datasource.InstallmentDataSource
import com.fidriyanto.banktracker.domain.model.Installment
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InstallmentRepositoryImpl @Inject constructor(
    private val dataSource: InstallmentDataSource,
) : InstallmentRepository {
    override suspend fun getAll(): Result<List<Installment>> =
        dataSource.fetchAll().map { dtos ->
            dtos.map { dto ->
                Installment(
                    id = dto.id,
                    merchant = dto.merchant,
                    wallet = dto.wallet,
                    installmentAmount = dto.installmentAmount,
                    totalInstallments = dto.totalInstallments,
                    currentStep = dto.currentStep,
                    dueDay = dto.dueDay,
                    status = dto.status,
                    excluded = dto.excluded,
                )
            }
        }

    // ac: installment-name-edit
    override suspend fun updateMerchant(id: Long, merchant: String): Result<Unit> =
        dataSource.updateMerchant(id, merchant)

    // ac: ac-isc-1, ac-isc-2
    override suspend fun incrementStep(id: Long, newStep: Int, newStatus: String): Result<Unit> =
        dataSource.incrementStep(id, newStep, newStatus)

    // ac: ac-isc-4
    override suspend fun setExcluded(id: Long, excluded: Boolean): Result<Unit> =
        dataSource.setExcluded(id, excluded)
}
