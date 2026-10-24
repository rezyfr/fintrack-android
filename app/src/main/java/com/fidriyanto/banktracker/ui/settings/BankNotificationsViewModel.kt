package com.fidriyanto.banktracker.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fidriyanto.banktracker.domain.model.Account
import com.fidriyanto.banktracker.domain.model.CustomBankPattern
import com.fidriyanto.banktracker.domain.model.KnownBank
import com.fidriyanto.banktracker.domain.usecase.BankParserSettingsUseCase
import com.fidriyanto.banktracker.domain.usecase.BankSetting
import com.fidriyanto.banktracker.domain.usecase.GetAccountsUseCase
import com.fidriyanto.banktracker.domain.usecase.ManageCustomBanksUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BankNotificationsViewModel @Inject constructor(
    private val settings: BankParserSettingsUseCase,
    getAccounts: GetAccountsUseCase,
    private val customBanksUseCase: ManageCustomBanksUseCase,
) : ViewModel() {
    val banks: StateFlow<List<BankSetting>> =
        settings.observe().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val accounts: StateFlow<List<Account>> =
        getAccounts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val customBanks: StateFlow<List<CustomBankPattern>> =
        customBanksUseCase.observe().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setCustomEnabled(id: String, on: Boolean) = viewModelScope.launch { customBanksUseCase.setEnabled(id, on) }
    fun choose(bank: KnownBank, account: Account?) = viewModelScope.launch { settings.choose(bank, account?.id) }
}
