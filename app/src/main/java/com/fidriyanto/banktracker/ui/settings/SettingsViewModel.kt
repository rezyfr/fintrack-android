package com.fidriyanto.banktracker.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fidriyanto.banktracker.data.repository.TransactionRepository
import com.fidriyanto.banktracker.domain.model.CurrencySettings
import com.fidriyanto.banktracker.domain.usecase.GetCurrencySettingsUseCase
import com.fidriyanto.banktracker.domain.usecase.SaveCurrencySettingsUseCase
import com.fidriyanto.banktracker.ui.common.CurrencySettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsState(
    val listener: ListenerStatus = ListenerStatus.NO_ACCESS,
    val isSyncing: Boolean = false,
    val isClearing: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val getCurrencySettings: GetCurrencySettingsUseCase,
    private val saveCurrencySettingsUseCase: SaveCurrencySettingsUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(SettingsState())
    val state = _state.asStateFlow()

    // How many transactions have not reached the server yet
    val pendingCount: StateFlow<Int> = transactionRepository.observePending().map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    // ac: configurable-currency-pair — the Settings screen lets the user rename the home/foreign
    // currency codes and labels
    val currencySettings: StateFlow<CurrencySettings> = MutableStateFlow(CurrencySettings()).also { flow ->
        viewModelScope.launch { getCurrencySettings().collect { flow.value = it; CurrencySettingsStore.hydrate(it) } }
    }.asStateFlow()

    fun saveCurrencySettings(settings: CurrencySettings) = viewModelScope.launch {
        saveCurrencySettingsUseCase(settings)
    }

    fun setListener(status: ListenerStatus) {
        _state.value = _state.value.copy(listener = status)
    }

    fun retryPendingSyncs() = viewModelScope.launch {
        _state.value = _state.value.copy(isSyncing = true)
        transactionRepository.retryFailedSyncs()
        _state.value = _state.value.copy(isSyncing = false)
    }

    fun markAllSynced() = viewModelScope.launch {
        _state.value = _state.value.copy(isClearing = true)
        transactionRepository.markAllSynced()
        _state.value = _state.value.copy(isClearing = false)
    }
}
