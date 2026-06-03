package com.fidriyanto.banktracker.ui.balances

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fidriyanto.banktracker.data.datasource.remote.dto.ReconciliationRowDto
import com.fidriyanto.banktracker.data.datasource.remote.dto.WalletBalanceDto
import com.fidriyanto.banktracker.data.repository.WalletBalanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

data class BalancesUiState(
    val balances: List<WalletBalanceDto> = emptyList(),
    val reconciliation: List<ReconciliationRowDto> = emptyList(),
    val isLoading: Boolean = false,
    val reconLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class BalancesViewModel @Inject constructor(
    private val repository: WalletBalanceRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(BalancesUiState(isLoading = true))
    val state: StateFlow<BalancesUiState> = _state.asStateFlow()

    private val _reconMonth = MutableStateFlow(currentMonth())
    val reconMonth: StateFlow<String> = _reconMonth.asStateFlow()

    init {
        loadBalances()
        loadReconciliation(currentMonth())
    }

    fun refresh() {
        loadBalances()
        loadReconciliation(_reconMonth.value)
    }

    // ac: view-wallet-balances — balances are fetched from the wallet_balances Supabase view on screen load
    private fun loadBalances() = viewModelScope.launch {
        _state.value = _state.value.copy(isLoading = true, error = null)
        repository.getBalances()
            .onSuccess { _state.value = _state.value.copy(balances = it, isLoading = false) }
            .onFailure { _state.value = _state.value.copy(error = it.message, isLoading = false) }
    }

    // ac: reconcile-wallet-monthly — month navigator lets user select which month to reconcile
    fun setReconMonth(month: String) {
        _reconMonth.value = month
        loadReconciliation(month)
    }

    private fun loadReconciliation(month: String) = viewModelScope.launch {
        _state.value = _state.value.copy(reconLoading = true)
        repository.getReconciliation(month)
            .onSuccess { _state.value = _state.value.copy(reconciliation = it, reconLoading = false) }
            .onFailure { _state.value = _state.value.copy(reconLoading = false) }
    }

    // ac: reconcile-wallet-monthly — saving an opening balance persists it to statement_balances
    fun saveOpeningBalance(walletId: String, openingBalance: Double) = viewModelScope.launch {
        repository.upsertStatementBalance(walletId, _reconMonth.value, openingBalance)
        loadReconciliation(_reconMonth.value)
    }

    fun monthDisplayLabel(ym: String): String {
        val (y, m) = ym.split("-").map { it.toInt() }
        val name = java.time.Month.of(m).getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
        return "$name ${(y % 100).toString().padStart(2, '0')}"
    }

    val availableMonths: List<String> = (0..23).map { i ->
        val d = LocalDate.now().minusMonths(i.toLong())
        "${d.year}-${d.monthValue.toString().padStart(2, '0')}"
    }

    companion object {
        fun currentMonth(): String {
            val d = LocalDate.now()
            return "${d.year}-${d.monthValue.toString().padStart(2, '0')}"
        }
    }
}
