package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.repository.AccountRepository
import com.fidriyanto.banktracker.domain.model.Account
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAccountsUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {
    // ac: manage-accounts — the Add Transaction form's account picker reflects the current list
    operator fun invoke(): Flow<List<Account>> = accountRepository.observeAll()

    suspend fun refresh(): Result<Unit> = accountRepository.refresh()
}
