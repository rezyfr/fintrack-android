package com.fidriyanto.banktracker.data.datasource.routing

import com.fidriyanto.banktracker.data.datasource.StorageSettingsDataSource
import com.fidriyanto.banktracker.data.datasource.offline.OfflineRuleDataSource
import com.fidriyanto.banktracker.data.datasource.remote.RuleRemoteDataSource
import com.fidriyanto.banktracker.data.datasource.remote.RuleRemoteDataSourceImpl
import com.fidriyanto.banktracker.data.model.RuleDocument
import javax.inject.Inject
import javax.inject.Singleton

// ac: offline-mode - sends each call to RuleRemoteDataSourceImpl or OfflineRuleDataSource, by the storage the user chose.
@Singleton
class RuleRouter @Inject constructor(
    storage: StorageSettingsDataSource,
    server: RuleRemoteDataSourceImpl,
    offline: OfflineRuleDataSource,
) : RuleRemoteDataSource {
    private val pick = StorageSwitch<RuleRemoteDataSource>(storage, server, offline)

    override suspend fun get(): RuleDocument? = pick().get()

    override suspend fun save(document: RuleDocument) = pick().save(document)
}
