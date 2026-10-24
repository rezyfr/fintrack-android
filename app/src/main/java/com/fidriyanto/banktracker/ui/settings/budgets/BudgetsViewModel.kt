package com.fidriyanto.banktracker.ui.settings.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fidriyanto.banktracker.domain.model.BudgetLine
import com.fidriyanto.banktracker.domain.usecase.ManageBudgetLinesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BudgetsState(
    val loading: Boolean = true,
    val failed: Boolean = false,
    val lines: List<BudgetLine> = emptyList(),
) {
    val flexible: List<BudgetLine> get() = lines.filter { it.kind != KIND_FIXED }.sortedBy { it.sortOrder }
    val fixed: List<BudgetLine> get() = lines.filter { it.kind == KIND_FIXED }.sortedBy { it.sortOrder }
}

@HiltViewModel
class BudgetsViewModel @Inject constructor(
    private val manage: ManageBudgetLinesUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(BudgetsState())
    val state: StateFlow<BudgetsState> = _state.asStateFlow()

    // The list is read again each time the screen opens, so a saved or deleted line shows at once.
    fun load() = viewModelScope.launch {
        _state.update { it.copy(failed = false) }
        manage.list()
            .onSuccess { lines -> _state.value = BudgetsState(loading = false, lines = lines) }
            .onFailure { _state.update { s -> s.copy(loading = false, failed = true) } }
    }
}
