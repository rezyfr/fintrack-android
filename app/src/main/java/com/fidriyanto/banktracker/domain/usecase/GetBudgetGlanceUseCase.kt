package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.repository.AccountRepository
import com.fidriyanto.banktracker.data.repository.BudgetLineRepository
import com.fidriyanto.banktracker.data.repository.CurrencySettingsRepository
import com.fidriyanto.banktracker.data.repository.TransactionRepository
import com.fidriyanto.banktracker.data.repository.WalletBalanceRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetBudgetGlanceUseCase @Inject constructor(
    private val budgetLineRepository: BudgetLineRepository,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val currencySettingsRepository: CurrencySettingsRepository,
    private val walletBalanceRepository: WalletBalanceRepository,
) {
    // ac: budget-combined-currency — a null currency gives both currencies' lines in the home currency
    // ac: budget-cycle-stepper — cycleOffset 0 is the cycle in progress, 1 the one before it
    suspend fun getGlance(currency: String?, cycleOffset: Int = 0): Result<BudgetGlance> = runCatching {
        loadBudgetGlance(
            currency, cycleOffset, budgetLineRepository, transactionRepository,
            accountRepository, currencySettingsRepository, walletBalanceRepository,
        )
    }
}
