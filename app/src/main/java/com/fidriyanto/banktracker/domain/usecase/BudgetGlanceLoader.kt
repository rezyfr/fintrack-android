package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.repository.AccountRepository
import com.fidriyanto.banktracker.data.repository.BudgetLineRepository
import com.fidriyanto.banktracker.data.repository.CurrencySettingsRepository
import com.fidriyanto.banktracker.data.repository.TransactionRepository
import com.fidriyanto.banktracker.data.repository.WalletBalanceRepository
import kotlinx.coroutines.flow.first

// ac: budget-combined-currency — a null currency gives both currencies' lines in the home currency
// ac: budget-cycle-stepper — cycleOffset 0 is the cycle in progress, 1 the one before it
// Loads what the budget figures need, then buildBudgetGlance makes the figures. The budget screen and
// the Ask screen both call this.
internal suspend fun loadBudgetGlance(
    currency: String?,
    cycleOffset: Int,
    budgetLineRepository: BudgetLineRepository,
    transactionRepository: TransactionRepository,
    accountRepository: AccountRepository,
    currencySettingsRepository: CurrencySettingsRepository,
    walletBalanceRepository: WalletBalanceRepository,
): BudgetGlance {
    val settings = currencySettingsRepository.observe().first()
    PayCycle.startDay = settings.cycleStartDay
    val cycle = PayCycle.cycleRange(cycleOffset)
    val prevCycle = PayCycle.cycleRange(cycleOffset + 1)
    // ac: configurable-currency-pair — a row's currency and the conversion direction come from
    // the configured pair and the accounts list instead of a hardcoded "BBL"/"THB"/"IDR"
    val currencyByWallet = accountRepository.observeAll().first().associate { it.id to it.currency }
    suspend fun cycleRows(c: PayCycle.Range) = transactionRepository
        .fetch(month = null, wallet = null, txType = null, dateFrom = c.fromIso, dateTo = c.toIso)
        .getOrElse { emptyList() }
    // ac: budget-cap-at-balance — checking accounts only: a card balance is owed money, and
    // investments are not for daily spending
    val balances = if (cycleOffset != 0) null else walletBalanceRepository.getBalances().getOrNull()
        ?.filter { it.type == "checking" }?.map { it.currency to it.balance }
    return buildBudgetGlance(
        BudgetGlanceInput(
            currency = currency,
            // ac: exchange-rate-from-transfers - each rate comes from the user's own recent transfers
            rates = transactionRepository.rateTable(currencyByWallet, settings),
            cycle = cycle,
            prevCycle = prevCycle,
            isCurrentCycle = cycleOffset == 0,
            lines = budgetLineRepository.getAll().getOrThrow(),
            currencyByWallet = currencyByWallet,
            rows = cycleRows(cycle),
            prevRows = cycleRows(prevCycle),
            // ac: spend-compare-more-cycles — the two cycles before the previous one, for the trend view
            olderCycles = (2..3).map { PayCycle.cycleRange(cycleOffset + it) }.map { it to cycleRows(it) },
            balances = balances,
        )
    )
}
