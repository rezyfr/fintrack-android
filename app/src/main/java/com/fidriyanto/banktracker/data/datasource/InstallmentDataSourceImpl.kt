package com.fidriyanto.banktracker.data.datasource

import com.fidriyanto.banktracker.data.datasource.remote.InstallmentApiService
import com.fidriyanto.banktracker.data.datasource.remote.dto.InstallmentCreateDto
import com.fidriyanto.banktracker.data.datasource.remote.dto.InstallmentDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InstallmentDataSourceImpl @Inject constructor(
    private val service: InstallmentApiService,
) : InstallmentDataSource {
    // ac: installment-overview
    override suspend fun fetchAll(): Result<List<InstallmentDto>> =
        runCatching { service.getInstallments() }

    // ac: installment-name-edit
    override suspend fun updateMerchant(id: Long, merchant: String): Result<Unit> =
        runCatching {
            val resp = service.updateInstallment(id, mapOf("merchant" to merchant))
            if (!resp.isSuccessful) error("HTTP ${resp.code()}")
        }

    // ac: ac-isc-1, ac-isc-2
    override suspend fun incrementStep(id: Long, newStep: Int, newStatus: String): Result<Unit> =
        runCatching {
            val resp = service.updateInstallment(id, mapOf("current_step" to newStep, "status" to newStatus))
            if (!resp.isSuccessful) error("HTTP ${resp.code()}")
        }

    // ac: ac-isc-4
    override suspend fun setExcluded(id: Long, excluded: Boolean): Result<Unit> =
        runCatching {
            val resp = service.updateInstallment(id, mapOf("excluded" to excluded))
            if (!resp.isSuccessful) error("HTTP ${resp.code()}")
        }

    // ac: installment-plan-add
    override suspend fun create(body: InstallmentCreateDto): Result<InstallmentDto> =
        runCatching {
            val resp = service.createInstallment(body)
            if (!resp.isSuccessful) error("HTTP ${resp.code()}")
            resp.body() ?: error("Empty response")
        }

    // ac: installment-plan-delete
    override suspend fun delete(id: Long): Result<Unit> =
        runCatching {
            val resp = service.deleteInstallment(id)
            if (!resp.isSuccessful) error("HTTP ${resp.code()}")
        }
}
