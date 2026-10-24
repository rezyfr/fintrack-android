package com.fidriyanto.banktracker.data.repository

import com.fidriyanto.banktracker.data.datasource.RuleDataSource
import com.fidriyanto.banktracker.data.datasource.remote.RuleRemoteDataSource
import com.fidriyanto.banktracker.data.model.RuleDocument
import com.fidriyanto.banktracker.domain.model.Rule
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RuleRepositoryImpl @Inject constructor(
    private val local: RuleDataSource,
    private val remote: RuleRemoteDataSource,
) : RuleRepository {
    override fun observe(): Flow<List<Rule>> = local.observe()

    // The phone keeps the edit first. The shared list follows. A failed upload leaves the edit marked,
    // and the next refresh sends it.
    override suspend fun replaceAll(rules: List<Rule>) {
        val doc = RuleDocument(rules, System.currentTimeMillis())
        local.save(doc, dirty = true)
        runCatching { remote.save(doc) }.onSuccess { local.markSynced() }
    }

    override suspend fun refresh(): Result<Unit> = runCatching {
        val mine = local.document()
        val dirty = local.isDirty()
        val theirs = remote.get()
        when {
            // Nobody has shared a list yet. A list that was never sent counts as the newest.
            theirs == null -> if (mine.rules.isNotEmpty()) push(mine)
            dirty && mine.updatedAtMs >= theirs.updatedAtMs -> push(mine)
            mine.updatedAtMs == 0L && mine.rules.isNotEmpty() -> push(mine)
            theirs.updatedAtMs > mine.updatedAtMs -> local.save(theirs, dirty = false)
        }
    }

    private suspend fun push(doc: RuleDocument) {
        val stamped = if (doc.updatedAtMs == 0L) doc.copy(updatedAtMs = System.currentTimeMillis()) else doc
        remote.save(stamped)
        local.save(stamped, dirty = false)
    }
}
