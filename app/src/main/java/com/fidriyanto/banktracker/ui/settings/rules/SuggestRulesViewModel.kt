package com.fidriyanto.banktracker.ui.settings.rules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fidriyanto.banktracker.domain.model.Category
import com.fidriyanto.banktracker.domain.model.DEFAULT_CATEGORIES
import com.fidriyanto.banktracker.domain.model.Rule
import com.fidriyanto.banktracker.domain.model.RuleAction
import com.fidriyanto.banktracker.domain.usecase.GetCategoriesUseCase
import com.fidriyanto.banktracker.domain.usecase.ManageRulesUseCase
import com.fidriyanto.banktracker.domain.usecase.RuleSuggestion
import com.fidriyanto.banktracker.domain.usecase.SuggestRulesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

enum class SuggestStatus { OPEN, ADDED, SKIPPED }

// One suggestion on the screen, the category the user picked, and the rule once it is added.
data class SuggestionItem(
    val suggestion: RuleSuggestion,
    val category: String? = null,
    val status: SuggestStatus = SuggestStatus.OPEN,
    val rule: Rule? = null,
)

data class SuggestState(val loading: Boolean = true, val failed: Boolean = false, val items: List<SuggestionItem> = emptyList())

@HiltViewModel
class SuggestRulesViewModel @Inject constructor(
    private val suggest: SuggestRulesUseCase,
    private val manage: ManageRulesUseCase,
    getCategories: GetCategoriesUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(SuggestState())
    val state: StateFlow<SuggestState> = _state.asStateFlow()

    val categories: StateFlow<List<Category>> = getCategories().map { it.ifEmpty { DEFAULT_CATEGORIES } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DEFAULT_CATEGORIES)

    init {
        load()
    }

    fun load() {
        _state.value = SuggestState()
        viewModelScope.launch {
            suggest()
                .onSuccess { list -> _state.value = SuggestState(false, false, list.map { SuggestionItem(it, it.hintCategory) }) }
                .onFailure { _state.value = SuggestState(false, true) }
        }
    }

    fun choose(key: String, category: String) = change(key) { it.copy(category = category) }

    fun skip(key: String) = change(key) { it.copy(status = SuggestStatus.SKIPPED) }

    // Saves the rule with the chosen category. A suggestion has no text of its own to name the rule, so the screen passes the name.
    fun add(key: String, name: String) {
        val item = _state.value.items.firstOrNull { it.suggestion.key == key } ?: return
        val category = item.category ?: return
        val rule = Rule(UUID.randomUUID().toString(), name, true, item.suggestion.conditions, RuleAction(category = category))
        viewModelScope.launch {
            manage.save(rule)
            change(key) { it.copy(status = SuggestStatus.ADDED, rule = rule) }
        }
    }

    private fun change(key: String, f: (SuggestionItem) -> SuggestionItem) =
        _state.update { s -> s.copy(items = s.items.map { if (it.suggestion.key == key) f(it) else it }) }
}
