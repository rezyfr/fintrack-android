package com.fidriyanto.banktracker.data.datasource

import com.fidriyanto.banktracker.data.model.RuleDocument
import com.fidriyanto.banktracker.domain.model.Rule
import kotlinx.coroutines.flow.Flow

interface RuleDataSource {
    // The rules in the order the user set. That order breaks a tie between two equally detailed rules.
    fun observe(): Flow<List<Rule>>

    // The rules, the time of the last edit, and whether the shared copy has not received that edit yet.
    suspend fun document(): RuleDocument
    suspend fun isDirty(): Boolean
    suspend fun save(document: RuleDocument, dirty: Boolean)
    suspend fun markSynced()
}
