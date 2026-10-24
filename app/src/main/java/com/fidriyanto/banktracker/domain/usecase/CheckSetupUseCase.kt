package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.repository.AccountRepository
import com.fidriyanto.banktracker.data.repository.CurrencySettingsRepository
import com.fidriyanto.banktracker.domain.model.SetupStatus
import kotlinx.coroutines.flow.first
import com.fidriyanto.banktracker.data.repository.StorageRepository
import com.fidriyanto.banktracker.domain.model.StorageMode
import javax.inject.Inject

class CheckSetupUseCase @Inject constructor(
    private val accounts: AccountRepository,
    private val currencySettings: CurrencySettingsRepository,
    private val storage: StorageRepository,
) {
    // A phone that already has accounts opens the app at once. A phone that keeps its data on the phone
    // and has none needs setup. Otherwise the server is read, and setup is needed when it has no accounts
    // either. A failed read never blocks the app, so an offline phone goes straight in.
    suspend operator fun invoke(): SetupStatus {
        if (accounts.observeAll().first().isNotEmpty()) return SetupStatus.DONE
        if (storage.get().mode == StorageMode.LOCAL) return SetupStatus.NEEDED
        currencySettings.refresh()
        val read = accounts.refresh()
        val empty = accounts.observeAll().first().isEmpty()
        return if (read.isSuccess && empty) SetupStatus.NEEDED else SetupStatus.DONE
    }
}
