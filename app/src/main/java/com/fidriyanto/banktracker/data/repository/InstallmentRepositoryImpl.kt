package com.fidriyanto.banktracker.data.repository

import com.fidriyanto.banktracker.data.datasource.InstallmentDataSource
import com.fidriyanto.banktracker.data.datasource.PendingInstallmentStepDataSource
import com.fidriyanto.banktracker.data.datasource.remote.dto.InstallmentCreateDto
import com.fidriyanto.banktracker.data.datasource.remote.mapper.toDomain
import com.fidriyanto.banktracker.domain.model.Installment
import com.fidriyanto.banktracker.domain.model.NewInstallment
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InstallmentRepositoryImpl @Inject constructor(
    private val dataSource: InstallmentDataSource,
    private val pendingStepDataSource: PendingInstallmentStepDataSource,
) : InstallmentRepository {
    private val linker = InstallmentLinker(dataSource, pendingStepDataSource)

    // ac: installment-link-step-retry — step updates that failed earlier are sent before the list is read
    override suspend fun getAll(): Result<List<Installment>> {
        linker.flushPending()
        return dataSource.fetchAll().map { dtos -> dtos.map { it.toDomain() } }
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

    // ac: installment-plan-add
    override suspend fun create(plan: NewInstallment): Result<Installment> =
        dataSource.create(
            InstallmentCreateDto(
                merchant = plan.merchant, merchantPattern = plan.merchantPattern, wallet = plan.wallet,
                installmentAmount = plan.installmentAmount, totalInstallments = plan.totalInstallments,
                currentStep = plan.currentStep, dueDay = plan.dueDay, status = plan.status,
            ),
        ).map { it.toDomain() }

    // ac: installment-plan-delete - a step update that waits for the deleted plan is dropped, or it would fail forever
    override suspend fun delete(id: Long): Result<Unit> =
        dataSource.delete(id).onSuccess { pendingStepDataSource.delete(id) }
}
