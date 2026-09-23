package com.fidriyanto.banktracker.ui.budgetglance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fidriyanto.banktracker.domain.usecase.BudgetGlance
import com.fidriyanto.banktracker.domain.usecase.GetBudgetGlanceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BudgetGlanceUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val currency: String = "IDR",
    val glance: BudgetGlance? = null,
)

@HiltViewModel
class BudgetGlanceViewModel @Inject constructor(
    private val getBudgetGlance: GetBudgetGlanceUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(BudgetGlanceUiState())
    val state: StateFlow<BudgetGlanceUiState> = _state.asStateFlow()

    init { load("IDR") }

    fun selectCurrency(currency: String) {
        if (currency == _state.value.currency && _state.value.glance != null) return
        load(currency)
    }

    private fun load(currency: String) {
        _state.value = BudgetGlanceUiState(loading = true, currency = currency)
        viewModelScope.launch {
            getBudgetGlance.getGlance(currency)
                .onSuccess { _state.value = BudgetGlanceUiState(loading = false, currency = currency, glance = it) }
                .onFailure { _state.value = BudgetGlanceUiState(loading = false, currency = currency, error = it.message) }
        }
    }
}
