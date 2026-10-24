package com.fidriyanto.banktracker.data.datasource.remote

import com.fidriyanto.banktracker.data.model.RuleDocument

interface RuleRemoteDataSource {
    // The shared rule list, or null when no phone has saved rules yet.
    suspend fun get(): RuleDocument?
    suspend fun save(document: RuleDocument)
}
