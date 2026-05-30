package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.repository.TransactionRepository
import com.fidriyanto.banktracker.domain.model.TransactionUiModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeedUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    fun observePending(): Flow<List<TransactionUiModel>> = repository.observePending()

    suspend fun syncTransaction(id: Long) = repository.syncTransaction(id)

    suspend fun updateAndSync(id: Long, item: String, category: String) =
        repository.updateAndSync(id, item, category)

    suspend fun fetchRemote(month: String?, wallet: String?, txType: String?) =
        repository.fetch(month, wallet, txType)
}
