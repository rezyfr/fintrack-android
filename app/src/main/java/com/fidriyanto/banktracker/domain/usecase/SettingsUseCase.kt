package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.repository.TransactionRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    suspend fun retryFailedSyncs() = transactionRepository.retryFailedSyncs()
    suspend fun markAllSynced() = transactionRepository.markAllSynced()
}
