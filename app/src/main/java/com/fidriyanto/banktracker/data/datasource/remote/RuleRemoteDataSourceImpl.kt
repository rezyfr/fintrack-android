package com.fidriyanto.banktracker.data.datasource.remote

import com.fidriyanto.banktracker.data.datasource.remote.dto.RulesDocDto
import com.fidriyanto.banktracker.data.model.RuleDocument
import javax.inject.Inject

class RuleRemoteDataSourceImpl @Inject constructor(
    private val service: RuleApiService,
) : RuleRemoteDataSource {
    // The server answers an empty document with time 0 when nobody has saved a list. That counts as no list.
    override suspend fun get(): RuleDocument? = service.getRules()
        .takeUnless { it.rules.isEmpty() && it.updatedAtMs == 0L }
        ?.let { RuleDocument(it.rules, it.updatedAtMs) }

    override suspend fun save(document: RuleDocument) {
        val response = service.putRules(RulesDocDto(rules = document.rules, updatedAtMs = document.updatedAtMs))
        if (!response.isSuccessful) error("API error: HTTP ${response.code()}")
    }
}
