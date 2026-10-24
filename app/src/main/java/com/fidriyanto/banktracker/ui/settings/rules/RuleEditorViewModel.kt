package com.fidriyanto.banktracker.ui.settings.rules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fidriyanto.banktracker.domain.model.Account
import com.fidriyanto.banktracker.domain.model.Category
import com.fidriyanto.banktracker.domain.model.DEFAULT_CATEGORIES
import com.fidriyanto.banktracker.domain.model.RuleInput
import com.fidriyanto.banktracker.domain.model.RuleOutcome
import com.fidriyanto.banktracker.domain.usecase.GetAccountsUseCase
import com.fidriyanto.banktracker.domain.usecase.GetCategoriesUseCase
import com.fidriyanto.banktracker.domain.usecase.GetFrequentCategoriesUseCase
import com.fidriyanto.banktracker.domain.usecase.ManageRulesUseCase
import com.fidriyanto.banktracker.domain.usecase.RuleEngine
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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID
import javax.inject.Inject

const val LAST_STEP = 2

data class TestResult(val matched: Boolean, val outcome: RuleOutcome)

data class EditorState(
    val loading: Boolean = true,
    val step: Int = 0,
    val draft: RuleDraft = RuleDraft(),
    val showErrors: Boolean = false,
    val testText: String = "",
    val testAmount: String = "",
    val testTime: String = "",
    val test: TestResult? = null,
    val closed: Boolean = false,
)

@HiltViewModel
class RuleEditorViewModel @Inject constructor(
    private val manage: ManageRulesUseCase,
    getCategories: GetCategoriesUseCase,
    getAccounts: GetAccountsUseCase,
    getFrequent: GetFrequentCategoriesUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(EditorState())
    val state: StateFlow<EditorState> = _state.asStateFlow()

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

    // A seed is a rule that is not saved yet, such as a suggestion. It opens as a new rule.
    fun load(ruleId: String?, seed: com.fidriyanto.banktracker.domain.model.Rule? = null) = viewModelScope.launch {
        val draft = ruleId?.let { manage.find(it) }?.let(RuleDraft::from) ?: seed?.let { RuleDraft.from(it).copy(id = null) } ?: RuleDraft()
        _state.value = EditorState(loading = false, draft = draft)
    }

    fun edit(change: (RuleDraft) -> RuleDraft) = _state.update { s -> s.copy(draft = change(s.draft), test = null) }

    fun setTest(text: String? = null, amount: String? = null, time: String? = null) = _state.update { s ->
        s.copy(testText = text ?: s.testText, testAmount = amount ?: s.testAmount, testTime = time ?: s.testTime).let { it.copy(test = runTest(it)) }
    }

    fun next() = _state.update { s ->
        val blocked = when (s.step) {
            0 -> s.draft.whenError() != null
            1 -> s.draft.thenError() != null
            else -> false
        }
        if (blocked) return@update s.copy(showErrors = true)
        val step = (s.step + 1).coerceAtMost(LAST_STEP)
        val entering = step == LAST_STEP && s.step != LAST_STEP
        val draft = if (entering && s.draft.name.isBlank()) s.draft.copy(name = s.draft.text.trim().ifBlank { s.draft.category.orEmpty() }) else s.draft
        val testText = if (entering && s.testText.isBlank()) draft.text else s.testText
        // The sample starts inside the rule, so the first try shows a match.
        val testAmount = if (entering && s.testAmount.isBlank() && draft.amountOn) (draft.amountMin.ifBlank { draft.amountMax }) else s.testAmount
        val testTime = if (entering && s.testTime.isBlank() && draft.timeOn) draft.timeFrom else s.testTime
        s.copy(step = step, draft = draft, showErrors = false, testText = testText, testAmount = testAmount, testTime = testTime).let { it.copy(test = if (entering) runTest(it) else it.test) }
    }

    // Returns true when the editor handled the back press by going to the step before.
    fun back(): Boolean {
        if (_state.value.step == 0) return false
        _state.update { it.copy(step = it.step - 1, showErrors = false) }
        return true
    }

    fun save() = viewModelScope.launch {
        val s = _state.value
        if (s.draft.whenError() != null || s.draft.thenError() != null || s.draft.name.isBlank()) {
            _state.update { it.copy(showErrors = true) }
            return@launch
        }
        manage.save(s.draft.toRule(UUID.randomUUID().toString()))
        _state.update { it.copy(closed = true) }
    }

    fun delete() = viewModelScope.launch {
        _state.value.draft.id?.let { manage.delete(it) }
        _state.update { it.copy(closed = true) }
    }

    // The draft alone decides here. The other rules are not part of the test.
    private fun runTest(s: EditorState): TestResult? {
        val d = s.draft
        if (d.whenError() != null || d.thenError() != null) return null
        val time = parseMinutes(s.testTime)?.let { LocalTime.of(it / 60, it % 60) } ?: LocalTime.now()
        val input = RuleInput(s.testText, s.testAmount.trim().toDoubleOrNull() ?: 0.0, d.type.takeIf { d.typeOn } ?: "expense", d.accounts.firstOrNull(), LocalDateTime.of(sampleDate(d), time))
        val rule = d.toRule("test")
        return TestResult(RuleEngine.matches(rule, input), RuleEngine.apply(listOf(rule), input))
    }

    // A day that fits the date of the rule, so the date does not stop the test from matching.
    private fun sampleDate(d: RuleDraft): LocalDate {
        if (!d.dateOn) return LocalDate.now()
        val start = d.year.toIntOrNull()?.let { LocalDate.of(it, 1, 1) } ?: LocalDate.now()
        return (0L..800L).map { start.plusDays(it) }.firstOrNull { x ->
            (d.day.isBlank() || x.dayOfMonth == d.day.toIntOrNull()) && (d.month.isBlank() || x.monthValue == d.month.toIntOrNull()) &&
                (d.year.isBlank() || x.year == d.year.toIntOrNull()) && (d.weekdays.isEmpty() || x.dayOfWeek.value in d.weekdays)
        } ?: LocalDate.now()
    }
}
