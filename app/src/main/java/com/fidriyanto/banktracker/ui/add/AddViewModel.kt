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

enum class Wallet(val id: String, val displayName: String, val currency: String) {
    BBL        ("BBL",        "Bangkok Bank",        "THB"),
    BCA        ("BCA",        "BCA Account",         "IDR"),
    MANDIRI    ("MANDIRI",    "Mandiri Account",      "IDR"),
    MANDIRI_CC ("MANDIRI_CC", "Mandiri Credit Card",  "IDR"),
    INVESTMENT ("INVESTMENT", "Investments",          "IDR"),
}

enum class TxType(val id: String, val displayName: String) {
    EXPENSE   ("expense",    "Expense"),
    INCOME    ("income",     "Income"),
    TRANSFER  ("transfer",   "Transfer"),
    INVESTMENT("investment", "Investment"),
}

data class AddFormState(
    val wallet: Wallet    = Wallet.BBL,
    val txType: TxType    = TxType.EXPENSE,
    val toWallet: Wallet? = null,
    val amount: String    = "",
    val description: String = "",
    val category: String  = "Other",
    val date: LocalDate   = LocalDate.now(),
    val isLoading: Boolean  = false,
    val successMessage: String? = null,
    val errorMessage: String?   = null
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
            s.wallet.currency == "THB" && s.txType == TxType.INCOME -> SheetTab.INCOME
            s.wallet.currency == "THB"                               -> SheetTab.EXPENSES
            s.txType == TxType.INCOME                                -> SheetTab.IDR_INCOME
            else                                                     -> SheetTab.IDR_EXPENSES
        }
        val row = SheetsRow(
            tab      = tab,
            date     = s.date,
            merchant = s.description,
            item     = s.description,
            amount   = amount,
            category = s.category,
            channel  = "Manual",
            wallet   = s.wallet.id,
            txType   = s.txType.id,
            toWallet = if (s.txType == TxType.TRANSFER) s.toWallet?.id else null
        )
        val result = repository.insertManual(row)
        _state.value = _state.value.copy(
            isLoading      = false,
            successMessage = if (result.isSuccess) "Saved and syncing!" else null,
            errorMessage   = if (result.isFailure) "Sync failed — saved offline" else null
        )
    }
}
