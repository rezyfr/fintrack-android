package com.fidriyanto.banktracker.data.repository

import com.fidriyanto.banktracker.domain.model.Rule
import kotlinx.coroutines.flow.Flow

interface RuleRepository {
    fun observe(): Flow<List<Rule>>
    suspend fun replaceAll(rules: List<Rule>)

    // Compares the phone with the shared list. The copy with the later edit wins.
    suspend fun refresh(): Result<Unit>
}
