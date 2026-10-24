package com.fidriyanto.banktracker.ui.settings.rules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fidriyanto.banktracker.domain.model.Rule
import com.fidriyanto.banktracker.domain.usecase.ApplyResult
import com.fidriyanto.banktracker.domain.usecase.ApplyRulesToPastUseCase
import com.fidriyanto.banktracker.domain.usecase.PastSupport
import com.fidriyanto.banktracker.domain.usecase.RuleChange
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PastPhase { LOADING, READY, APPLYING, DONE, UNDONE, ERROR }

data class PastState(
    val phase: PastPhase = PastPhase.LOADING,
    val support: PastSupport = PastSupport.OK,
    val days: Int = 90,
    val changes: List<RuleChange> = emptyList(),
    val progress: Int = 0,
    val result: ApplyResult? = null,
)

@HiltViewModel
class PastRulesViewModel @Inject constructor(
    private val past: ApplyRulesToPastUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(PastState())
    val state: StateFlow<PastState> = _state.asStateFlow()
    private var rule: Rule? = null

    fun start(rule: Rule) {
        this.rule = rule
        load(_state.value.days)
    }

    fun load(days: Int) {
        val r = rule ?: return
        val support = past.support(r)
        _state.update { it.copy(days = days, support = support, phase = if (support == PastSupport.OK) PastPhase.LOADING else PastPhase.READY, changes = emptyList()) }
        if (support != PastSupport.OK) return
        viewModelScope.launch {
            past.preview(r, days)
                .onSuccess { c -> _state.update { it.copy(phase = PastPhase.READY, changes = c) } }
                .onFailure { _state.update { it.copy(phase = PastPhase.ERROR) } }
        }
    }

    fun apply() = run(PastPhase.DONE) { c, progress -> past.apply(c, progress) }

    fun undo() = run(PastPhase.UNDONE) { c, progress -> past.undo(c, progress) }

    private fun run(end: PastPhase, work: suspend (List<RuleChange>, (Int) -> Unit) -> ApplyResult) {
        val changes = _state.value.changes
        _state.update { it.copy(phase = PastPhase.APPLYING, progress = 0) }
        viewModelScope.launch {
            val result = work(changes) { n -> _state.update { it.copy(progress = n) } }
            _state.update { it.copy(phase = end, result = result) }
        }
    }
}
