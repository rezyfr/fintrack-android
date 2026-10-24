package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.repository.AccountRepository
import com.fidriyanto.banktracker.data.repository.CurrencySettingsRepository
import com.fidriyanto.banktracker.data.repository.TransactionRepository
import com.fidriyanto.banktracker.domain.model.RateTable
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

// ac: exchange-rate-from-transfers - the rates for screens that combine currencies outside a use case of
// their own, such as the Home spending chart
@Singleton
class GetExchangeRateUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val currencySettingsRepository: CurrencySettingsRepository,
) {
    suspend fun rates(): RateTable {
        val currencyByWallet = accountRepository.observeAll().first().associate { it.id to it.currency }
        return transactionRepository.rateTable(currencyByWallet, currencySettingsRepository.observe().first())
    }
}
