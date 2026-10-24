package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.repository.AccountRepository
import com.fidriyanto.banktracker.data.repository.CurrencySettingsRepository
import com.fidriyanto.banktracker.domain.model.Account
import com.fidriyanto.banktracker.domain.model.CurrencyOverride
import com.fidriyanto.banktracker.domain.model.CurrencySettings
import com.fidriyanto.banktracker.domain.model.SetupInput
import javax.inject.Inject

class CompleteSetupUseCase @Inject constructor(
    private val accounts: AccountRepository,
    private val currencySettings: CurrencySettingsRepository,
) {
    // Saves the currencies and the accounts the user chose. The currency codes are the book names until
    // the user renames them in Settings.
    suspend operator fun invoke(input: SetupInput): Result<Unit> {
        SetupValidator.validate(input)?.let { return Result.failure(IllegalArgumentException(it.name)) }
        val home = CurrencyDetect.normalize(input.homeCode)
        val others = input.accounts.map { CurrencyDetect.normalize(it.currency) }.filter { it != home }.distinct()
        currencySettings.save(
            CurrencySettings(
                homeCode = home, homeLabel = home, cycleStartDay = input.cycleStartDay,
                overrides = others.map { CurrencyOverride(it, it, input.rates[it]?.takeIf { r -> r > 0 }) },
            )
        )
        val taken = mutableSetOf<String>()
        return runCatching {
            input.accounts.forEachIndexed { index, a ->
                val id = accountIdFor(a.name, taken).also { taken += it }
                val account = Account(id, a.name.trim(), CurrencyDetect.normalize(a.currency), isDefault = false, type = a.type, sortOrder = index)
                accounts.save(account).getOrThrow()
            }
        }
    }
}
