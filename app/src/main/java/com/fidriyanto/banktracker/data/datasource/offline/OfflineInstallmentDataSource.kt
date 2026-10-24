package com.fidriyanto.banktracker.data.datasource.offline

import com.fidriyanto.banktracker.data.datasource.InstallmentDataSource
import com.fidriyanto.banktracker.data.datasource.remote.dto.InstallmentCreateDto
import com.fidriyanto.banktracker.data.datasource.remote.dto.InstallmentDto
import com.fidriyanto.banktracker.data.db.LedgerDao
import com.fidriyanto.banktracker.data.db.LedgerInstallmentEntity
import javax.inject.Inject
import javax.inject.Singleton

// ac: offline-mode - installment plans in the offline store. A change to a plan that does not exist fails,
// as the server answers 404 for it.
@Singleton
class OfflineInstallmentDataSource @Inject constructor(
    private val dao: LedgerDao,
) : InstallmentDataSource {
    override suspend fun fetchAll(): Result<List<InstallmentDto>> = runCatching { dao.installments().map { it.toDto() } }

    override suspend fun updateMerchant(id: Long, merchant: String): Result<Unit> = runCatching {
        found(dao.setMerchant(id, merchant), id)
    }

    override suspend fun incrementStep(id: Long, newStep: Int, newStatus: String): Result<Unit> = runCatching {
        found(dao.setStep(id, newStep, newStatus), id)
    }

    override suspend fun setExcluded(id: Long, excluded: Boolean): Result<Unit> = runCatching {
        found(dao.setExcluded(id, excluded), id)
    }

    override suspend fun create(body: InstallmentCreateDto): Result<InstallmentDto> = runCatching {
        val row = LedgerInstallmentEntity(
            merchant = body.merchant, merchantPattern = body.merchantPattern, wallet = body.wallet,
            installmentAmount = body.installmentAmount, totalInstallments = body.totalInstallments,
            currentStep = body.currentStep, dueDay = body.dueDay, status = body.status, excluded = body.excluded,
        )
        row.copy(id = dao.insertInstallment(row)).toDto()
    }

    override suspend fun delete(id: Long): Result<Unit> = runCatching { found(dao.deleteInstallment(id), id) }

    private fun found(rows: Int, id: Long) {
        check(rows > 0) { "Installment $id not found" }
    }
}
