package com.fidriyanto.banktracker.data.datasource

import com.fidriyanto.banktracker.data.datasource.remote.dto.InstallmentCreateDto
import com.fidriyanto.banktracker.data.datasource.remote.dto.InstallmentDto

interface InstallmentDataSource {
    suspend fun fetchAll(): Result<List<InstallmentDto>>
    // ac: installment-name-edit
    suspend fun updateMerchant(id: Long, merchant: String): Result<Unit>
    // ac: ac-isc-1, ac-isc-2
    suspend fun incrementStep(id: Long, newStep: Int, newStatus: String): Result<Unit>
    // ac: ac-isc-4
    suspend fun setExcluded(id: Long, excluded: Boolean): Result<Unit>
    // ac: installment-plan-add
    suspend fun create(body: InstallmentCreateDto): Result<InstallmentDto>
    // ac: installment-plan-delete
    suspend fun delete(id: Long): Result<Unit>
}
