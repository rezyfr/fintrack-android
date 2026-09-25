package com.fidriyanto.banktracker.ui.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fidriyanto.banktracker.data.model.LedgerTab
import com.fidriyanto.banktracker.data.model.TransactionEntry
import com.fidriyanto.banktracker.data.repository.MerchantHistoryRepository
import com.fidriyanto.banktracker.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

enum class Wallet(val id: String, val displayName: String, val currency: String) {
    BBL        ("BBL",        "Bangkok Bank",        "THB"),
    BCA        ("BCA",        "BCA Account",         "IDR"),
    BCA_CC     ("BCA_CC",     "BCA Credit Card",     "IDR"),
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
    // ac: add-transfer-target-amount — received amount for a cross-currency transfer (destination currency)
    val toAmount: String  = "",
    val amount: String    = "",
    val description: String = "",
    val category: String  = "Other",
    val subcategory: String? = null,
    val date: LocalDate   = LocalDate.now(),
    val isLoading: Boolean  = false,
    val successMessage: String? = null,
    val errorMessage: String?   = null
)

@HiltViewModel
class AddViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val merchantHistoryRepository: MerchantHistoryRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(AddFormState())
    val state = _state.asStateFlow()

    private val _dismissedQuery = MutableStateFlow<String?>(null)

    val merchantSuggestions: StateFlow<List<String>> = combine(
        merchantHistoryRepository.observe(),
        _state,
        _dismissedQuery,
    ) { history, s, dismissedQuery ->
        val query = s.description.trim()
        when {
            query == dismissedQuery -> emptyList()
            query.isEmpty()        -> history
            else                   -> history.filter { it.contains(query, ignoreCase = true) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun update(block: AddFormState.() -> AddFormState) {
        val prev = _state.value
        val next = prev.block()
        if (next.description != prev.description) _dismissedQuery.value = null
        _state.value = next
    }

    fun dismissSuggestions() { _dismissedQuery.value = _state.value.description.trim() }

    // Clear the form back to defaults so the next time the add bottom sheet opens it starts fresh.
    fun reset() {
        _state.value = AddFormState()
        _dismissedQuery.value = null
    }

    fun submit() = viewModelScope.launch {
        val s = _state.value
        val amount = s.amount.toDoubleOrNull() ?: run {
            _state.value = s.copy(errorMessage = "Enter a valid amount"); return@launch
        }
        _state.value = s.copy(isLoading = true, errorMessage = null)

        val tab = when {
            s.wallet.currency == "THB" && s.txType == TxType.INCOME -> LedgerTab.INCOME
            s.wallet.currency == "THB"                               -> LedgerTab.EXPENSES
            s.txType == TxType.INCOME                                -> LedgerTab.IDR_INCOME
            else                                                     -> LedgerTab.IDR_EXPENSES
        }
        // ac: add-transaction-date — the submitted transaction uses the selected date, not the current date
        val entry = TransactionEntry(
            tab      = tab,
            date     = s.date,
            item     = s.description,
            amount   = amount,
            category = s.category,
            wallet   = s.wallet.id,
            txType   = s.txType.id,
            toWallet = if (s.txType == TxType.TRANSFER) s.toWallet?.id else null,
            // ac: add-transfer-target-amount — store the received amount only for cross-currency transfers
            toAmount = if (s.txType == TxType.TRANSFER && s.toWallet != null && s.toWallet.currency != s.wallet.currency)
                s.toAmount.toDoubleOrNull() else null,
            // ac: add-transaction-subcategory — the submitted transaction stores the chosen subcategory
            subcategory = s.subcategory,
        )
        val result = transactionRepository.insertManual(entry)
        if (result.isSuccess) merchantHistoryRepository.save(s.description)
        _state.value = _state.value.copy(
            isLoading      = false,
            successMessage = if (result.isSuccess) "Saved and syncing!" else null,
            errorMessage   = if (result.isFailure) "Sync failed — saved offline" else null
        )
    }
}
