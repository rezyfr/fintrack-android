package com.fidriyanto.banktracker.data.datasource

import com.fidriyanto.banktracker.data.db.PendingInstallmentStepDao
import com.fidriyanto.banktracker.data.db.PendingInstallmentStepEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PendingInstallmentStepDataSourceImpl @Inject constructor(
    private val dao: PendingInstallmentStepDao,
) : PendingInstallmentStepDataSource {
    override suspend fun getAll(): List<PendingInstallmentStepEntity> = dao.getAll()

    override suspend fun save(installmentId: Long, newStep: Int, newStatus: String) {
        val waiting = dao.get(installmentId)
        if (waiting == null || waiting.newStep < newStep) {
            dao.upsert(PendingInstallmentStepEntity(installmentId, newStep, newStatus))
        }
    }

    override suspend fun delete(installmentId: Long) = dao.delete(installmentId)
}
