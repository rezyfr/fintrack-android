package com.fidriyanto.banktracker.ui.settings.custombanks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fidriyanto.banktracker.domain.model.Account
import com.fidriyanto.banktracker.domain.usecase.GetAccountsUseCase
import com.fidriyanto.banktracker.domain.usecase.ManageCustomBanksUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

const val CUSTOM_BANK_LAST_STEP = 2

data class CustomBankEditorState(
    val loading: Boolean = true,
    val step: Int = 0,
    val draft: CustomBankDraft = CustomBankDraft(),
    val showErrors: Boolean = false,
    val closed: Boolean = false,
)

@HiltViewModel
class CustomBankEditorViewModel @Inject constructor(
    private val manage: ManageCustomBanksUseCase,
    getAccounts: GetAccountsUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(CustomBankEditorState())
    val state: StateFlow<CustomBankEditorState> = _state.asStateFlow()

    val accounts: StateFlow<List<Account>> = getAccounts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun load(patternId: String?) = viewModelScope.launch {
        val existing = patternId?.let { manage.find(it) }
        val first = accounts.value.firstOrNull { it.isDefault } ?: accounts.value.firstOrNull()
        val draft = existing?.let(CustomBankDraft::from) ?: CustomBankDraft(walletId = first?.id.orEmpty())
        _state.value = CustomBankEditorState(loading = false, draft = draft)
    }

    fun edit(change: (CustomBankDraft) -> CustomBankDraft) = _state.update { it.copy(draft = change(it.draft)) }

    fun next() = _state.update { s ->
        if (s.step == 0 && s.draft.text.isBlank()) return@update s.copy(showErrors = true)
        if (s.step == 1 && s.draft.markError() != null) return@update s.copy(showErrors = true)
        val draft = if (s.step == 1) s.draft.withDefaults() else s.draft
        s.copy(step = (s.step + 1).coerceAtMost(CUSTOM_BANK_LAST_STEP), draft = draft, showErrors = false)
    }

    // Returns true when the editor handled the back press by going to the step before.
    fun back(): Boolean {
        if (_state.value.step == 0) return false
        _state.update { it.copy(step = it.step - 1, showErrors = false) }
        return true
    }

    fun save() {
        val d = _state.value.draft
        val pattern = d.toPattern(UUID.randomUUID().toString())
        if (pattern == null || d.nameError() || d.walletId.isBlank()) {
            _state.update { it.copy(showErrors = true) }
            return
        }
        viewModelScope.launch {
            manage.save(pattern)
            _state.update { it.copy(closed = true) }
        }
    }

    fun delete() = viewModelScope.launch {
        _state.value.draft.id?.let { manage.delete(it) }
        _state.update { it.copy(closed = true) }
    }
}
