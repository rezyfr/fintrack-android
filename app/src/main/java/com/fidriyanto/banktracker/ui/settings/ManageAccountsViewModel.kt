package com.fidriyanto.banktracker.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fidriyanto.banktracker.domain.model.Account
import com.fidriyanto.banktracker.domain.model.AccountType
import com.fidriyanto.banktracker.domain.usecase.DeleteAccountUseCase
import com.fidriyanto.banktracker.domain.usecase.GetAccountsUseCase
import com.fidriyanto.banktracker.domain.usecase.SaveAccountUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManageAccountsViewModel @Inject constructor(
    private val getAccounts: GetAccountsUseCase,
    private val saveAccount: SaveAccountUseCase,
    private val deleteAccount: DeleteAccountUseCase,
) : ViewModel() {
    // ac: manage-accounts — the Settings screen lists every account with its currency
    val accounts: StateFlow<List<Account>> =
        getAccounts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        viewModelScope.launch { getAccounts.refresh() }
    }

    // ac: manage-accounts — the user can add a custom account with a name and a currency
    fun addAccount(name: String, currency: String, type: AccountType, openingBalance: Double) = viewModelScope.launch {
        saveAccount(existingId = null, name = name, currency = currency, type = type, openingBalance = openingBalance)
            .onFailure { _error.value = it.message }
    }

    // ac: manage-accounts — the user can rename a custom account they added
    // ac: account-opening-balance — and change what it held before its first transaction
    fun editAccount(account: Account, newName: String, openingBalance: Double) = viewModelScope.launch {
        saveAccount(existingId = account.id, name = newName, currency = account.currency, type = account.type, openingBalance = openingBalance)
            .onFailure { _error.value = it.message }
    }

    fun delete(account: Account) = viewModelScope.launch {
        deleteAccount(account).onFailure { _error.value = it.message }
    }

    fun dismissError() { _error.value = null }
}
