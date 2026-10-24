package com.fidriyanto.banktracker.ui.settings.installmentplans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fidriyanto.banktracker.domain.model.Account
import com.fidriyanto.banktracker.domain.model.Installment
import com.fidriyanto.banktracker.domain.usecase.GetAccountsUseCase
import com.fidriyanto.banktracker.domain.usecase.GetInstallmentsUseCase
import com.fidriyanto.banktracker.domain.usecase.ManageInstallmentsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InstallmentPlansState(
    val plans: List<Installment> = emptyList(),
    val loading: Boolean = true,
    val loadFailed: Boolean = false,
    val deleteFailed: Boolean = false,
)

@HiltViewModel
class InstallmentPlansViewModel @Inject constructor(
    private val getInstallments: GetInstallmentsUseCase,
    private val manage: ManageInstallmentsUseCase,
    getAccounts: GetAccountsUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(InstallmentPlansState())
    val state: StateFlow<InstallmentPlansState> = _state.asStateFlow()
    val accounts: StateFlow<List<Account>> = getAccounts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init { load() }

    // Active plans first, then completed ones. Each group is in the order the server gives.
    fun load() = viewModelScope.launch {
        _state.update { it.copy(loading = true, loadFailed = false) }
        getInstallments()
            .onSuccess { all -> _state.update { it.copy(plans = all.sortedBy { p -> p.isCompleted }, loading = false) } }
            .onFailure { _state.update { it.copy(loading = false, loadFailed = true) } }
    }

    fun delete(id: Long) = viewModelScope.launch {
        manage.delete(id)
            .onSuccess { _state.update { s -> s.copy(plans = s.plans.filter { it.id != id }, deleteFailed = false) } }
            .onFailure { _state.update { it.copy(deleteFailed = true) } }
    }

    fun dismissDeleteError() = _state.update { it.copy(deleteFailed = false) }
}
