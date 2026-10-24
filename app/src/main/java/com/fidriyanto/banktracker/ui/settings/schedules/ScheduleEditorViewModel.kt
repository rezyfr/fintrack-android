package com.fidriyanto.banktracker.ui.settings.schedules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fidriyanto.banktracker.domain.model.Account
import com.fidriyanto.banktracker.domain.model.Category
import com.fidriyanto.banktracker.domain.model.DEFAULT_CATEGORIES
import com.fidriyanto.banktracker.domain.model.INCOME_CATEGORIES
import com.fidriyanto.banktracker.domain.model.Installment
import com.fidriyanto.banktracker.domain.usecase.GetAccountsUseCase
import com.fidriyanto.banktracker.domain.usecase.GetCategoriesUseCase
import com.fidriyanto.banktracker.domain.usecase.GetFrequentCategoriesUseCase
import com.fidriyanto.banktracker.domain.usecase.GetInstallmentsUseCase
import com.fidriyanto.banktracker.domain.usecase.ManageSchedulesUseCase
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
import java.util.UUID
import javax.inject.Inject

const val LAST_STEP = 2

data class ScheduleEditorState(
    val loading: Boolean = true,
    val step: Int = 0,
    val draft: ScheduleDraft = ScheduleDraft(),
    val showErrors: Boolean = false,
    val closed: Boolean = false,
)

@HiltViewModel
class ScheduleEditorViewModel @Inject constructor(
    private val manage: ManageSchedulesUseCase,
    getCategories: GetCategoriesUseCase,
    getAccounts: GetAccountsUseCase,
    getFrequent: GetFrequentCategoriesUseCase,
    private val getInstallments: GetInstallmentsUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(ScheduleEditorState())
    val state: StateFlow<ScheduleEditorState> = _state.asStateFlow()

    val categories: StateFlow<List<Category>> = getCategories().map { it.ifEmpty { DEFAULT_CATEGORIES } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DEFAULT_CATEGORIES)
    val accounts: StateFlow<List<Account>> = getAccounts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ac: category-picker - the most used category names for each type id. Read once when the editor opens.
    private val _frequent = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val frequentByType: StateFlow<Map<String, List<String>>> = _frequent.asStateFlow()

    init {
        viewModelScope.launch {
            val expense = getCategories().first().ifEmpty { DEFAULT_CATEGORIES }.map { it.name }
            _frequent.value = getFrequent.byType(mapOf("expense" to expense, "income" to INCOME_CATEGORIES))
        }
    }

    // ac: schedule-installment-link - the plans a payment can be linked to. They are read once when the editor opens.
    private val _plans = MutableStateFlow<List<Installment>>(emptyList())
    val plans: StateFlow<List<Installment>> = _plans.asStateFlow()

    fun load(scheduleId: String?, template: ScheduleTemplate?) = viewModelScope.launch {
        val existing = scheduleId?.let { manage.find(it) }
        _plans.value = getInstallments().getOrNull().orEmpty().filter { !it.isCompleted }
        val first = accounts.value.firstOrNull { it.isDefault } ?: accounts.value.firstOrNull()
        // The installment idea starts on the account of the first plan, so the link choice is already there.
        val start = if (template == ScheduleTemplate.INSTALLMENT) _plans.value.firstOrNull()?.wallet ?: first?.id else first?.id
        val draft = existing?.let(ScheduleDraft::from)
            ?: ScheduleDraft.from(template ?: ScheduleTemplate.OTHER).copy(walletId = start.orEmpty())
        _state.value = ScheduleEditorState(loading = false, draft = draft)
    }

    fun edit(change: (ScheduleDraft) -> ScheduleDraft) = _state.update { it.copy(draft = change(it.draft)) }

    fun next() = _state.update { s ->
        if (s.step == 0 && s.draft.whatError() != null) return@update s.copy(showErrors = true)
        if (s.step == 1 && s.draft.endError()) return@update s.copy(showErrors = true)
        s.copy(step = (s.step + 1).coerceAtMost(LAST_STEP), showErrors = false)
    }

    // Returns true when the editor handled the back press by going to the step before.
    fun back(): Boolean {
        if (_state.value.step == 0) return false
        _state.update { it.copy(step = it.step - 1, showErrors = false) }
        return true
    }

    fun save() = viewModelScope.launch {
        manage.save(_state.value.draft.toSchedule(UUID.randomUUID().toString()))
        _state.update { it.copy(closed = true) }
    }

    fun delete() = viewModelScope.launch {
        _state.value.draft.id?.let { manage.delete(it) }
        _state.update { it.copy(closed = true) }
    }
}
