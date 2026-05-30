package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.repository.TransactionFetchRepository
import com.fidriyanto.banktracker.data.repository.TransactionRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeedUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val fetchRepository: TransactionFetchRepository
) {
    fun observePending() = transactionRepository.observePending()
    suspend fun syncTransaction(id: Long) = transactionRepository.syncTransaction(id)
    suspend fun updateAndSync(id: Long, item: String, category: String) =
        transactionRepository.updateAndSync(id, item, category)
    suspend fun fetchRemote(month: String?, wallet: String?, txType: String?) =
        fetchRepository.fetch(month, wallet, txType)
}
