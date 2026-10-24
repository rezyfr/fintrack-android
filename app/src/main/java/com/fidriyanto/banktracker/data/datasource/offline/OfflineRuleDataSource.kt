package com.fidriyanto.banktracker.data.datasource.offline

import com.fidriyanto.banktracker.data.datasource.remote.RuleRemoteDataSource
import com.fidriyanto.banktracker.data.model.RuleDocument
import javax.inject.Inject
import javax.inject.Singleton

// ac: offline-mode - there is no shared rule list offline. The phone's own copy is the only one.
@Singleton
class OfflineRuleDataSource @Inject constructor() : RuleRemoteDataSource {
    override suspend fun get(): RuleDocument? = null

    override suspend fun save(document: RuleDocument) = Unit
}
