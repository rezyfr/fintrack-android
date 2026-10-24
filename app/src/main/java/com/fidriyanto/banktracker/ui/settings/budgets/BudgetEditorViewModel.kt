package com.fidriyanto.banktracker.ui.settings.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fidriyanto.banktracker.domain.model.Account
import com.fidriyanto.banktracker.domain.model.BudgetLine
import com.fidriyanto.banktracker.domain.model.Category
import com.fidriyanto.banktracker.domain.model.DEFAULT_CATEGORIES
import com.fidriyanto.banktracker.domain.usecase.BudgetLineRules
import com.fidriyanto.banktracker.domain.usecase.GetAccountsUseCase
import com.fidriyanto.banktracker.domain.usecase.GetCategoriesUseCase
import com.fidriyanto.banktracker.domain.usecase.GetFrequentCategoriesUseCase
import com.fidriyanto.banktracker.domain.usecase.ManageBudgetLinesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

const val BUDGET_LAST_STEP = 1

data class BudgetEditorState(
    val loading: Boolean = true,
    val step: Int = 0,
    val draft: BudgetDraft = BudgetDraft(),
    val showErrors: Boolean = false,
    // Every line that exists, to warn about a twin
    val lines: List<BudgetLine> = emptyList(),
    val saving: Boolean = false,
    val saveFailed: Boolean = false,
    val closed: Boolean = false,
) {
    // Another line that has the same category, subcategory and currency. It is a warning, not an error.
    val duplicate: BudgetLine? get() = BudgetLineRules.duplicateOf(draft.toBudgetLine(), lines)
}

@HiltViewModel
class BudgetEditorViewModel @Inject constructor(
    private val manage: ManageBudgetLinesUseCase,
    getCategories: GetCategoriesUseCase,
    getAccounts: GetAccountsUseCase,
    getFrequent: GetFrequentCategoriesUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(BudgetEditorState())
    val state: StateFlow<BudgetEditorState> = _state.asStateFlow()

    val categories: StateFlow<List<Category>> = getCategories().map { it.ifEmpty { DEFAULT_CATEGORIES } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DEFAULT_CATEGORIES)
    val accounts: StateFlow<List<Account>> = getAccounts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ac: category-picker - the most used category names for each type id. Read once when the editor opens.
    private val _frequent = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val frequentByType: StateFlow<Map<String, List<String>>> = _frequent.asStateFlow()

    init {
        viewModelScope.launch {
            val expense = getCategories().first().ifEmpty { DEFAULT_CATEGORIES }.map { it.name }
            _frequent.value = getFrequent.byType(mapOf("expense" to expense))
        }
    }

    fun load(lineId: Long?) = viewModelScope.launch {
        val lines = manage.list().getOrNull().orEmpty()
        val existing = lineId?.let { id -> lines.firstOrNull { it.id == id } }
        if (lineId != null && existing == null) {
            _state.update { it.copy(closed = true) }
            return@launch
        }
        val first = accounts.value.firstOrNull { it.isDefault } ?: accounts.value.firstOrNull()
        val draft = existing?.let(BudgetDraft::from) ?: BudgetDraft(currency = first?.currency.orEmpty())
        _state.value = BudgetEditorState(loading = false, draft = draft, lines = lines)
    }

    fun edit(change: (BudgetDraft) -> BudgetDraft) = _state.update { it.copy(draft = change(it.draft), saveFailed = false) }

    fun next() = _state.update { s ->
        if (s.step == 0 && s.draft.whatError() != null) return@update s.copy(showErrors = true)
        s.copy(step = (s.step + 1).coerceAtMost(BUDGET_LAST_STEP), showErrors = false)
    }

    // Returns true when the editor handled the back press by going to the step before.
    fun back(): Boolean {
        if (_state.value.step == 0) return false
        _state.update { it.copy(step = it.step - 1, showErrors = false) }
        return true
    }

    fun save() {
        val s = _state.value
        if (s.saving) return
        if (s.draft.howError() != null) {
            _state.update { it.copy(showErrors = true) }
            return
        }
        submit { manage.save(s.draft.toBudgetLine()) }
    }

    fun delete() {
        val id = _state.value.draft.original?.id ?: return
        if (_state.value.saving) return
        submit { manage.delete(id) }
    }

    private fun submit(call: suspend () -> Result<Unit>) {
        _state.update { it.copy(saving = true, saveFailed = false) }
        viewModelScope.launch {
            call()
                .onSuccess { _state.update { s -> s.copy(saving = false, closed = true) } }
                .onFailure { _state.update { s -> s.copy(saving = false, saveFailed = true) } }
        }
    }
}
