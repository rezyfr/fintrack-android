package com.fidriyanto.banktracker.data.repository

import com.fidriyanto.banktracker.data.datasource.AutoAddedDataSource
import com.fidriyanto.banktracker.data.db.AutoAddedEntity
import com.fidriyanto.banktracker.domain.model.AutoAddedItem
import com.fidriyanto.banktracker.domain.model.AutoSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutoAddedRepositoryImpl @Inject constructor(
    private val local: AutoAddedDataSource,
) : AutoAddedRepository {
    override suspend fun add(item: String, amount: Double, txType: String, wallet: String?, source: AutoSource) =
        local.add(AutoAddedEntity(addedAtMs = System.currentTimeMillis(), item = item, amount = amount, txType = txType, wallet = wallet, source = source.name))

    override fun observeActive(): Flow<List<AutoAddedItem>> = local.observeActive().map { rows ->
        rows.map { AutoAddedItem(it.id, it.addedAtMs, it.item, it.amount, it.txType, it.wallet, AutoSource.entries.firstOrNull { s -> s.name == it.source } ?: AutoSource.SCHEDULED) }
    }

    override suspend fun dismissAll() = local.dismissAll()
    override suspend fun prune(cutoffMs: Long) = local.prune(cutoffMs)
}
