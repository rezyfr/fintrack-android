package com.fidriyanto.banktracker.ui.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fidriyanto.banktracker.data.model.SheetTab
import com.fidriyanto.banktracker.data.model.SheetsRow
import com.fidriyanto.banktracker.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class AddFormState(
    val account: String = "THB",
    val type: String = "Expense",
    val amount: String = "",
    val description: String = "",
    val category: String = "Other",
    val date: LocalDate = LocalDate.now(),
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class AddViewModel @Inject constructor(
    private val repository: TransactionRepository
) : ViewModel() {
    private val _state = MutableStateFlow(AddFormState())
    val state = _state.asStateFlow()

    fun update(block: AddFormState.() -> AddFormState) { _state.value = _state.value.block() }

    fun submit() = viewModelScope.launch {
        val s = _state.value
        val amount = s.amount.toDoubleOrNull() ?: run {
            _state.value = s.copy(errorMessage = "Enter a valid amount"); return@launch
        }
        _state.value = s.copy(isLoading = true, errorMessage = null)
        val tab = when {
            s.account == "THB" && s.type == "Expense" -> SheetTab.EXPENSES
            s.account == "THB" && s.type == "Income"  -> SheetTab.INCOME
            s.account == "IDR" && s.type == "Expense" -> SheetTab.IDR_EXPENSES
            else                                       -> SheetTab.IDR_INCOME
        }
        val row = SheetsRow(tab, s.date, s.description, amount, s.category)
        val result = repository.insertManual(row)
        _state.value = _state.value.copy(
            isLoading = false,
            successMessage = if (result.isSuccess) "Synced to Sheets!" else null,
            errorMessage = if (result.isFailure) "Sync failed — saved offline" else null
        )
    }
}
