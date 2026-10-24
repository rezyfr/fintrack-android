package com.fidriyanto.banktracker.ui.balances

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fidriyanto.banktracker.data.datasource.remote.dto.WalletBalanceDto
import com.fidriyanto.banktracker.data.repository.WalletBalanceRepository
import com.fidriyanto.banktracker.domain.usecase.GetCurrencySettingsUseCase
import com.fidriyanto.banktracker.domain.usecase.GetGrowthHistoryUseCase
import com.fidriyanto.banktracker.domain.usecase.GrowthHistory
import com.fidriyanto.banktracker.ui.common.CurrencySettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BalancesUiState(
    val balances: List<WalletBalanceDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    // ac: account-growth-chart — null until it loads, and stays null when it fails, which hides the card
    val growth: GrowthHistory? = null,
)

@HiltViewModel
class BalancesViewModel @Inject constructor(
    private val repository: WalletBalanceRepository,
    private val getCurrencySettings: GetCurrencySettingsUseCase,
    private val getGrowthHistory: GetGrowthHistoryUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(BalancesUiState(isLoading = true))
    val state: StateFlow<BalancesUiState> = _state.asStateFlow()

    init {
        // ac: configurable-currency-pair — hydrates CurrencySettingsStore for the net-worth row
        viewModelScope.launch { getCurrencySettings().collect(CurrencySettingsStore::hydrate) }
        loadBalances()
    }

    fun refresh() {
        loadBalances()
    }

    // ac: view-wallet-balances — balances are fetched from the wallet balances endpoint on screen load
    private fun loadBalances() = viewModelScope.launch {
        // The balances do not wait for the chart. A failed chart request leaves the screen as it was.
        launch { getGrowthHistory().onSuccess { _state.value = _state.value.copy(growth = it) } }
        _state.value = _state.value.copy(isLoading = true, error = null)
        repository.getBalances()
            .onSuccess { _state.value = _state.value.copy(balances = it, isLoading = false) }
            .onFailure { _state.value = _state.value.copy(error = it.message, isLoading = false) }
    }
}
