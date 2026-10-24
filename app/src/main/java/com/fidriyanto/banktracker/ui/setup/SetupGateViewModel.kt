package com.fidriyanto.banktracker.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fidriyanto.banktracker.domain.model.SetupStatus
import com.fidriyanto.banktracker.domain.usecase.CheckSetupUseCase
import com.fidriyanto.banktracker.domain.usecase.GetAccountsUseCase
import com.fidriyanto.banktracker.domain.usecase.GetCategoriesUseCase
import com.fidriyanto.banktracker.domain.usecase.GetCurrencySettingsUseCase
import com.fidriyanto.banktracker.domain.usecase.ManageRulesUseCase
import com.fidriyanto.banktracker.ui.common.AccountStore
import com.fidriyanto.banktracker.ui.common.CurrencySettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ac: first-run-setup - decides whether the app opens or the setup screen shows first. It also loads
// the accounts and currency settings once, so every screen reads them from the first frame.
@HiltViewModel
class SetupGateViewModel @Inject constructor(
    private val checkSetup: CheckSetupUseCase,
    private val getAccounts: GetAccountsUseCase,
    private val getCurrencySettings: GetCurrencySettingsUseCase,
    private val rules: ManageRulesUseCase,
    private val getCategories: GetCategoriesUseCase,
) : ViewModel() {
    // Null while the check runs.
    private val _status = MutableStateFlow<SetupStatus?>(null)
    val status: StateFlow<SetupStatus?> = _status.asStateFlow()

    init {
        viewModelScope.launch { getAccounts().collect(AccountStore::hydrate) }
        viewModelScope.launch { getCurrencySettings().collect(CurrencySettingsStore::hydrate) }
        viewModelScope.launch { _status.value = checkSetup() }
        // The phone opens on what it has. These refresh it from the server in the background.
        viewModelScope.launch {
            getAccounts.refresh()
            getCurrencySettings.refresh()
            rules.refresh()
        }
    }

    // The data may now come from a different place than at start, for example a server that setup just
    // connected, or the phone. Reading it again fills every screen. On the phone, it also writes the default
    // categories.
    fun onSetupFinished() {
        _status.value = SetupStatus.DONE
        viewModelScope.launch {
            getAccounts.refresh()
            getCurrencySettings.refresh()
            rules.refresh()
            getCategories.refresh()
        }
    }
}
