package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.repository.RuleRepository
import com.fidriyanto.banktracker.domain.model.Rule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

// The rule list: read it, save a rule, delete a rule and move a rule up or down.
class ManageRulesUseCase @Inject constructor(
    private val rules: RuleRepository,
) {
    fun observe(): Flow<List<Rule>> = rules.observe()

    // A new rule goes to the end of the list. An existing rule keeps its place.
    suspend fun save(rule: Rule) {
        val all = rules.observe().first()
        rules.replaceAll(if (all.any { it.id == rule.id }) all.map { if (it.id == rule.id) rule else it } else all + rule)
    }

    suspend fun delete(id: String) = rules.replaceAll(rules.observe().first().filterNot { it.id == id })

    // A step of -1 moves the rule up. A step of 1 moves it down.
    suspend fun move(id: String, step: Int) {
        val all = rules.observe().first().toMutableList()
        val from = all.indexOfFirst { it.id == id }
        val to = from + step
        if (from < 0 || to !in all.indices) return
        all.add(to, all.removeAt(from))
        rules.replaceAll(all)
    }

    // Compares the phone with the shared list. The copy with the later edit wins.
    suspend fun refresh(): Result<Unit> = rules.refresh()

    suspend fun find(id: String): Rule? = rules.observe().first().firstOrNull { it.id == id }
}
