package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.repository.TransactionRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeleteTransactionUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(id: Long): Result<Unit> = repository.deleteTransaction(id)
}
