package com.fidriyanto.banktracker.data.datasource.routing

import com.fidriyanto.banktracker.data.datasource.InstallmentDataSource
import com.fidriyanto.banktracker.data.datasource.InstallmentDataSourceImpl
import com.fidriyanto.banktracker.data.datasource.StorageSettingsDataSource
import com.fidriyanto.banktracker.data.datasource.offline.OfflineInstallmentDataSource
import com.fidriyanto.banktracker.data.datasource.remote.dto.InstallmentCreateDto
import com.fidriyanto.banktracker.data.datasource.remote.dto.InstallmentDto
import javax.inject.Inject
import javax.inject.Singleton

// ac: offline-mode - sends each call to InstallmentDataSourceImpl or OfflineInstallmentDataSource, by the storage the user chose.
@Singleton
class InstallmentRouter @Inject constructor(
    storage: StorageSettingsDataSource,
    server: InstallmentDataSourceImpl,
    offline: OfflineInstallmentDataSource,
) : InstallmentDataSource {
    private val pick = StorageSwitch<InstallmentDataSource>(storage, server, offline)

    override suspend fun fetchAll(): Result<List<InstallmentDto>> = pick().fetchAll()

    override suspend fun updateMerchant(id: Long, merchant: String): Result<Unit> = pick().updateMerchant(id, merchant)

    override suspend fun incrementStep(id: Long, newStep: Int, newStatus: String): Result<Unit> = pick().incrementStep(id, newStep, newStatus)

    override suspend fun setExcluded(id: Long, excluded: Boolean): Result<Unit> = pick().setExcluded(id, excluded)

    override suspend fun create(body: InstallmentCreateDto): Result<InstallmentDto> = pick().create(body)

    override suspend fun delete(id: Long): Result<Unit> = pick().delete(id)
}
