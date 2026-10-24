package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.repository.AccountRepository
import com.fidriyanto.banktracker.domain.model.Account
import javax.inject.Inject

class DeleteAccountUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {
    // ac: manage-accounts — a default account cannot be deleted, since bank notification parsing
    // depends on its fixed id
    suspend operator fun invoke(account: Account): Result<Unit> {
        if (account.isDefault) {
            return Result.failure(IllegalStateException("${account.displayName} is a default account and cannot be deleted"))
        }
        return accountRepository.delete(account.id)
    }
}
