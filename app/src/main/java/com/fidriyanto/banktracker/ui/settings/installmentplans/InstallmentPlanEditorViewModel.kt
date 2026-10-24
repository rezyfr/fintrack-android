package com.fidriyanto.banktracker.ui.settings.installmentplans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fidriyanto.banktracker.domain.model.Account
import com.fidriyanto.banktracker.domain.model.AccountType
import com.fidriyanto.banktracker.domain.model.Installment
import com.fidriyanto.banktracker.domain.model.TransactionUiModel
import com.fidriyanto.banktracker.domain.usecase.GetAccountsUseCase
import com.fidriyanto.banktracker.domain.usecase.GetInstallmentsUseCase
import com.fidriyanto.banktracker.domain.usecase.GetRecentPaymentsUseCase
import com.fidriyanto.banktracker.domain.usecase.ManageInstallmentsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

const val PLAN_LAST_STEP = 1

// The payments the user can start from. Null means the dialog is closed.
data class PaymentPicker(val loading: Boolean = true, val failed: Boolean = false, val rows: List<TransactionUiModel> = emptyList())

data class InstallmentPlanEditorState(
    val step: Int = 0,
    val draft: InstallmentPlanDraft = InstallmentPlanDraft(),
    val showErrors: Boolean = false,
    val saving: Boolean = false,
    val saveFailed: Boolean = false,
    val closed: Boolean = false,
    val picker: PaymentPicker? = null,
)

@HiltViewModel
class InstallmentPlanEditorViewModel @Inject constructor(
    private val manage: ManageInstallmentsUseCase,
    private val getInstallments: GetInstallmentsUseCase,
    private val getRecentPayments: GetRecentPaymentsUseCase,
    getAccounts: GetAccountsUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(InstallmentPlanEditorState())
    val state: StateFlow<InstallmentPlanEditorState> = _state.asStateFlow()

    // All accounts, credit accounts first
    val accounts: StateFlow<List<Account>> = getAccounts().map { list -> list.sortedBy { it.type != AccountType.CREDIT } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // The plans that exist now, read once when the editor opens. They feed the duplicate warning.
    private val _plans = MutableStateFlow<List<Installment>>(emptyList())
    val plans: StateFlow<List<Installment>> = _plans.asStateFlow()

    init { viewModelScope.launch { _plans.value = getInstallments().getOrNull().orEmpty() } }

    fun edit(change: (InstallmentPlanDraft) -> InstallmentPlanDraft) = _state.update { it.copy(draft = change(it.draft)) }

    fun next() = _state.update { s ->
        if (s.draft.planError() != null) s.copy(showErrors = true) else s.copy(step = PLAN_LAST_STEP, showErrors = false)
    }

    // Returns true when the editor handled the back press by going to the step before.
    fun back(): Boolean {
        if (_state.value.step == 0) return false
        _state.update { it.copy(step = 0, showErrors = false) }
        return true
    }

    fun openPicker() {
        val wallet = _state.value.draft.walletId
        if (wallet.isBlank()) return
        _state.update { it.copy(picker = PaymentPicker()) }
        viewModelScope.launch {
            val result = getRecentPayments(wallet)
            _state.update { it.copy(picker = PaymentPicker(loading = false, failed = result.isFailure, rows = result.getOrNull().orEmpty())) }
        }
    }

    fun closePicker() = _state.update { it.copy(picker = null) }

    fun pick(tx: TransactionUiModel) = _state.update { it.copy(draft = it.draft.withPayment(tx), picker = null) }

    fun save() {
        val s = _state.value
        if (s.saving) return
        if (s.draft.error() != null) {
            _state.update { it.copy(showErrors = true) }
            return
        }
        _state.update { it.copy(saving = true, saveFailed = false) }
        viewModelScope.launch {
            manage.create(s.draft.toNew())
                .onSuccess { _state.update { it.copy(saving = false, closed = true) } }
                .onFailure { _state.update { it.copy(saving = false, saveFailed = true) } }
        }
    }
}
