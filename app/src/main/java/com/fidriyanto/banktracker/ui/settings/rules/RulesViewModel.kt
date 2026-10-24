package com.fidriyanto.banktracker.ui.settings.rules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fidriyanto.banktracker.domain.model.Rule
import com.fidriyanto.banktracker.domain.usecase.ManageRulesUseCase
import com.fidriyanto.banktracker.domain.usecase.RuleConflicts
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RulesState(val loaded: Boolean = false, val rules: List<Rule> = emptyList(), val conflicts: List<Pair<Rule, Rule>> = emptyList())

@HiltViewModel
class RulesViewModel @Inject constructor(
    private val manage: ManageRulesUseCase,
) : ViewModel() {
    val state: StateFlow<RulesState> = manage.observe()
        .map { RulesState(true, it, RuleConflicts.find(it)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RulesState())

    init {
        viewModelScope.launch { manage.refresh() }
    }

    fun toggle(rule: Rule) = viewModelScope.launch { manage.save(rule.copy(enabled = !rule.enabled)) }

    fun move(rule: Rule, step: Int) = viewModelScope.launch { manage.move(rule.id, step) }
}
