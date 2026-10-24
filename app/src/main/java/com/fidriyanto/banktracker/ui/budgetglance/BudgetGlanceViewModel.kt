package com.fidriyanto.banktracker.ui.budgetglance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fidriyanto.banktracker.domain.usecase.BudgetGlance
import com.fidriyanto.banktracker.domain.usecase.GetBudgetGlanceUseCase
import com.fidriyanto.banktracker.domain.usecase.GetCurrencySettingsUseCase
import com.fidriyanto.banktracker.ui.common.BookLoad
import com.fidriyanto.banktracker.ui.common.BookStore
import com.fidriyanto.banktracker.ui.common.CurrencySettingsStore
import com.fidriyanto.banktracker.ui.common.currencyCode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BudgetGlanceUiState(
    val loading: Boolean = true,
    val error: String? = null,
    // ac: budget-combined-currency - null means all currencies combined in the home currency
    val currency: String? = null,
    // ac: budget-cycle-stepper — 0 is the pay cycle in progress, 1 the one before it
    val cycleOffset: Int = 0,
    val glance: BudgetGlance? = null,
)

@HiltViewModel
class BudgetGlanceViewModel @Inject constructor(
    private val getBudgetGlance: GetBudgetGlanceUseCase,
    private val getCurrencySettings: GetCurrencySettingsUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(BudgetGlanceUiState())
    val state: StateFlow<BudgetGlanceUiState> = _state.asStateFlow()

    init {
        // ac: book-switch-transition — the book switch waits for this page to finish loading before it reveals it
        viewModelScope.launch { state.collect { BookLoad.report(BookLoad.BUDGET, it.loading) } }
        // ac: configurable-currency-pair — the initial currency is the configured home currency
        // instead of a hardcoded "IDR" literal
        viewModelScope.launch { getCurrencySettings().collect(CurrencySettingsStore::hydrate) }
        // ac: currency-books — the budget shows the book picked by the switch at the top of the app.
        // COMBINED is both currencies in the home currency.
        viewModelScope.launch { BookStore.book.collect { load(it.currencyCode(), _state.value.cycleOffset) } }
    }

    // ac: budget-cycle-stepper — step one pay cycle back (+1) or forward (-1), not past the cycle in progress
    fun stepCycle(delta: Int) {
        val offset = (_state.value.cycleOffset + delta).coerceAtLeast(0)
        if (offset != _state.value.cycleOffset) load(_state.value.currency, offset)
    }

    // ac: budget-glance-reloads-on-resume — refetch the current currency's budget lines
    fun refresh() = load(_state.value.currency, _state.value.cycleOffset)

    private fun load(currency: String?, cycleOffset: Int = 0) {
        _state.update { it.copy(loading = true, currency = currency, cycleOffset = cycleOffset) }
        viewModelScope.launch {
            val result = getBudgetGlance.getGlance(currency, cycleOffset)
            // A later selection replaces this one, so a slow, older result is dropped.
            _state.update {
                if (it.currency != currency || it.cycleOffset != cycleOffset) it
                else BudgetGlanceUiState(
                    loading = false, currency = currency, cycleOffset = cycleOffset,
                    glance = result.getOrNull(), error = result.exceptionOrNull()?.message,
                )
            }
        }
    }

    override fun onCleared() {
        BookLoad.report(BookLoad.BUDGET, false)
        super.onCleared()
    }
}
