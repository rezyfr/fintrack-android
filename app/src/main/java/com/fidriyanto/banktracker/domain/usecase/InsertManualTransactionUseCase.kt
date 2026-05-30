package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.model.TransactionEntry
import com.fidriyanto.banktracker.data.repository.TransactionRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InsertManualTransactionUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    suspend fun execute(entry: TransactionEntry): Result<Unit> = repository.insertManual(entry)
}
