package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.model.TransactionEdit
import com.fidriyanto.banktracker.data.repository.TransactionRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EditTransactionUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(id: Long, edit: TransactionEdit): Result<Unit> =
        repository.editTransaction(id, edit)
}
