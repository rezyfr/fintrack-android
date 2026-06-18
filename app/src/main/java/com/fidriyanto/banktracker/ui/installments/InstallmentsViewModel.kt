package com.fidriyanto.banktracker.ui.installments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fidriyanto.banktracker.domain.model.Installment
import com.fidriyanto.banktracker.domain.usecase.GetInstallmentsUseCase
import com.fidriyanto.banktracker.domain.usecase.IncrementInstallmentStepUseCase
import com.fidriyanto.banktracker.domain.usecase.SetInstallmentExcludedUseCase
import com.fidriyanto.banktracker.domain.usecase.UpdateInstallmentNameUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InstallmentsUiState(
    // ac: installment-overview — installments sorted by next upcoming payment date
    val active: List<Installment> = emptyList(),
    // ac: installment-overview — completed installments shown in a separate section
    val completed: List<Installment> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    // ac: installment-name-edit — which card's edit dialog is open + pre-fill name
    val editingId: Long? = null,
    val editingInitialName: String = "",
)

@HiltViewModel
class InstallmentsViewModel @Inject constructor(
    private val getInstallments: GetInstallmentsUseCase,
    private val updateInstallmentName: UpdateInstallmentNameUseCase,
    private val incrementStep: IncrementInstallmentStepUseCase,
    private val setExcluded: SetInstallmentExcludedUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(InstallmentsUiState())
    val state: StateFlow<InstallmentsUiState> = _state.asStateFlow()

    init { load() }

    fun load() = viewModelScope.launch {
        _state.update { it.copy(isLoading = true, error = null) }
        getInstallments()
            .onSuccess { all -> applyAll(all) }
            .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
    }

    private fun applyAll(all: List<Installment>) {
        _state.update {
            it.copy(
                active = all.filter { i -> !i.isCompleted }.sortedBy { i -> i.nextDueDate() },
                completed = all.filter { i -> i.isCompleted },
                isLoading = false,
            )
        }
    }

    // ac: installment-name-edit — tapping the edit icon pre-fills the dialog with the current merchant name
    fun startEdit(id: Long, currentName: String) {
        _state.update { it.copy(editingId = id, editingInitialName = currentName) }
    }

    fun cancelEdit() {
        _state.update { it.copy(editingId = null, editingInitialName = "") }
    }

    // ac: installment-name-edit — saving PATCHes the installments table and updates the card immediately
    fun saveEdit(name: String) = viewModelScope.launch {
        val id = _state.value.editingId ?: return@launch
        val trimmed = name.trim()
        if (trimmed.isBlank()) return@launch
        cancelEdit()
        updateInstallmentName(id, trimmed).onSuccess {
            _state.update { s ->
                s.copy(
                    active = s.active.map { i -> if (i.id == id) i.copy(merchant = trimmed) else i },
                    completed = s.completed.map { i -> if (i.id == id) i.copy(merchant = trimmed) else i },
                )
            }
        }
    }

    // ac: ac-isc-1, ac-isc-2
    fun onIncrementStep(installment: Installment) = viewModelScope.launch {
        val newStep = installment.currentStep + 1
        val newStatus = if (newStep >= installment.totalInstallments) "completed" else installment.status
        val updated = installment.copy(currentStep = newStep, status = newStatus)
        _state.update { s ->
            s.copy(
                active = if (updated.isCompleted)
                    s.active.filter { it.id != installment.id }
                else
                    s.active.map { if (it.id == installment.id) updated else it },
                completed = if (updated.isCompleted) (s.completed + updated) else s.completed,
            )
        }
        incrementStep(updated).onFailure {
            _state.update { s ->
                s.copy(
                    active = (s.active.filter { it.id != installment.id } + installment)
                        .sortedBy { it.nextDueDate() },
                    completed = s.completed.filter { it.id != installment.id },
                )
            }
        }
    }

    // ac: ac-isc-4
    fun onSetExcluded(id: Long, excluded: Boolean) = viewModelScope.launch {
        _state.update { s ->
            s.copy(active = s.active.map { if (it.id == id) it.copy(excluded = excluded) else it })
        }
        setExcluded(id, excluded)
    }
}
