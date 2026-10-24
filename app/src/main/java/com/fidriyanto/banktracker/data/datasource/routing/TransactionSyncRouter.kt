package com.fidriyanto.banktracker.data.datasource.routing

import com.fidriyanto.banktracker.data.datasource.StorageSettingsDataSource
import com.fidriyanto.banktracker.data.datasource.TransactionSyncDataSource
import com.fidriyanto.banktracker.data.datasource.TransactionSyncDataSourceImpl
import com.fidriyanto.banktracker.data.datasource.offline.OfflineTransactionSyncDataSource
import com.fidriyanto.banktracker.data.model.TransactionEdit
import com.fidriyanto.banktracker.data.model.TransactionEntry
import javax.inject.Inject
import javax.inject.Singleton

// ac: offline-mode - sends each call to TransactionSyncDataSourceImpl or OfflineTransactionSyncDataSource, by the storage the user chose.
@Singleton
class TransactionSyncRouter @Inject constructor(
    storage: StorageSettingsDataSource,
    server: TransactionSyncDataSourceImpl,
    offline: OfflineTransactionSyncDataSource,
) : TransactionSyncDataSource {
    private val pick = StorageSwitch<TransactionSyncDataSource>(storage, server, offline)

    override suspend fun sync(entry: TransactionEntry): Result<Unit> = pick().sync(entry)

    override suspend fun delete(id: Long): Result<Unit> = pick().delete(id)

    override suspend fun update(id: Long, edit: TransactionEdit): Result<Unit> = pick().update(id, edit)

    override suspend fun unlinkInstallment(id: Long): Result<Unit> = pick().unlinkInstallment(id)

    override suspend fun updateCategoryBatch(remoteIds: List<Long>, category: String): Result<Unit> = pick().updateCategoryBatch(remoteIds, category)
}
