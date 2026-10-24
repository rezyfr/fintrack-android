package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.repository.AccountRepository
import com.fidriyanto.banktracker.domain.model.Account
import com.fidriyanto.banktracker.domain.model.AccountType
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class SaveAccountUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {
    // ac: manage-accounts — the user can add a custom account with a name and a currency
    // ac: manage-accounts — the user can rename a custom account they added
    // ac: account-opening-balance — the account's opening balance is saved with it. A credit account
    // keeps 0, because the chart and the field leave credit cards out.
    // Adding: existingId is null, a fresh id is derived from the name. Renaming: existingId is
    // kept so transactions already stored against it keep resolving to the same account.
    suspend operator fun invoke(
        existingId: String?,
        name: String,
        currency: String,
        type: AccountType = AccountType.CHECKING,
        openingBalance: Double = 0.0,
    ): Result<Unit> {
        if (name.isBlank()) return Result.failure(IllegalArgumentException("Name is required"))
        if (currency.isBlank()) return Result.failure(IllegalArgumentException("Currency is required"))
        val all = accountRepository.observeAll().first()
        val id = existingId ?: accountIdFor(name, all.map { it.id }.toSet())
        // A renamed account keeps its place in the list. A new account goes last.
        val existing = all.firstOrNull { it.id == id }
        val order = existing?.sortOrder ?: ((all.maxOfOrNull { it.sortOrder } ?: -1) + 1)
        return accountRepository.save(
            Account(
                id = id,
                displayName = name.trim(),
                currency = currency.trim().uppercase(),
                // Editing a default account keeps it default.
                isDefault = existing?.isDefault ?: false,
                type = type,
                sortOrder = order,
                openingBalance = if (type == AccountType.CREDIT) 0.0 else openingBalance,
            ),
        )
    }
}
