package com.fidriyanto.banktracker.data.repository

import com.fidriyanto.banktracker.domain.model.Installment

interface InstallmentRepository {
    suspend fun getAll(): Result<List<Installment>>
    // ac: installment-name-edit
    suspend fun updateMerchant(id: Long, merchant: String): Result<Unit>
    // ac: ac-isc-1, ac-isc-2
    suspend fun incrementStep(id: Long, newStep: Int, newStatus: String): Result<Unit>
    // ac: ac-isc-4
    suspend fun setExcluded(id: Long, excluded: Boolean): Result<Unit>
}
