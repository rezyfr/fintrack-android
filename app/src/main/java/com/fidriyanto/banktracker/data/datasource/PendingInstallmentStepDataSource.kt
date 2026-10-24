package com.fidriyanto.banktracker.data.datasource

import com.fidriyanto.banktracker.data.db.PendingInstallmentStepEntity

// ac: installment-link-step-retry — installment step updates waiting to be sent to the server
interface PendingInstallmentStepDataSource {
    suspend fun getAll(): List<PendingInstallmentStepEntity>

    // Keeps the higher step when the installment already has a waiting update
    suspend fun save(installmentId: Long, newStep: Int, newStatus: String)

    suspend fun delete(installmentId: Long)
}
