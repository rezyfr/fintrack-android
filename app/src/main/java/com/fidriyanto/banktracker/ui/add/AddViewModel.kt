package com.fidriyanto.banktracker.ui.add

import com.fidriyanto.banktracker.domain.usecase.GetBudgetLinesUseCase
import com.fidriyanto.banktracker.domain.model.BudgetLine
import com.fidriyanto.banktracker.ui.common.currencyCode
import com.fidriyanto.banktracker.ui.common.BookStore
import com.fidriyanto.banktracker.ui.common.Book
import com.fidriyanto.banktracker.domain.model.Account
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fidriyanto.banktracker.data.model.ledgerTabFor
import com.fidriyanto.banktracker.data.model.TransactionEntry
import com.fidriyanto.banktracker.data.repository.TransactionRepository
import com.fidriyanto.banktracker.domain.usecase.GetAccountsUseCase
import com.fidriyanto.banktracker.domain.usecase.GetCategoriesUseCase
import com.fidriyanto.banktracker.domain.usecase.GetFrequentCategoriesUseCase
import com.fidriyanto.banktracker.domain.model.DEFAULT_CATEGORIES
import com.fidriyanto.banktracker.domain.model.INCOME_CATEGORIES
import kotlinx.coroutines.flow.first
import com.fidriyanto.banktracker.domain.usecase.GetCurrencySettingsUseCase
import com.fidriyanto.banktracker.domain.usecase.ApplyRulesUseCase
import com.fidriyanto.banktracker.domain.usecase.SuggestCategoryUseCase
import com.fidriyanto.banktracker.domain.model.RuleInput
import java.time.LocalDateTime
import java.time.LocalTime
import com.fidriyanto.banktracker.domain.model.CategorySuggestion
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import com.fidriyanto.banktracker.ui.common.AccountStore
import com.fidriyanto.banktracker.ui.common.CategoryStore
import com.fidriyanto.banktracker.ui.common.CurrencySettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val SUGGEST_DELAY_MS = 800L

enum class TxType(val id: String, val displayName: String) {
    EXPENSE ("expense", "Expense"),
    INCOME ("income", "Income"),
    TRANSFER ("transfer", "Transfer"),
    INVESTMENT("investment", "Investment"),
}

data class AddFormState(
    // ac: transaction-form-redesign — the form values shared with the Edit sheet
    val form: TxFormValues = TxFormValues(),
    val isLoading: Boolean = false,
    // ac: transaction-form-validation — the first missing or invalid field after Save
    val validationError: TxFormError? = null,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class AddViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val getAccounts: GetAccountsUseCase,
    private val getCategories: GetCategoriesUseCase,
    private val getCurrencySettings: GetCurrencySettingsUseCase,
    private val getBudgetLines: GetBudgetLinesUseCase,
    private val suggestCategory: SuggestCategoryUseCase,
    private val applyRules: ApplyRulesUseCase,
    private val getFrequent: GetFrequentCategoriesUseCase,
) : ViewModel() {
    // ac: on-device-ai — a category the model suggests for the description. The user accepts it with one tap.
    private val _suggestion = MutableStateFlow<CategorySuggestion?>(null)
    val suggestion: StateFlow<CategorySuggestion?> = _suggestion.asStateFlow()
    private var suggestJob: Job? = null

    // ac: budget-by-category — the budget lines, so the form can show which budget an expense counts toward
    private val _budgetLines = MutableStateFlow<List<BudgetLine>>(emptyList())
    val budgetLines: StateFlow<List<BudgetLine>> = _budgetLines.asStateFlow()

    // ac: category-picker — the most used category names for each type id. Loaded once when the form opens.
    private val _frequent = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val frequentByType: StateFlow<Map<String, List<String>>> = _frequent.asStateFlow()

    private val _state = MutableStateFlow(AddFormState())
    val state = _state.asStateFlow()

    init {
        // ac: manage-accounts — hydrates AccountStore so the account picker reflects the current list
        // ac: manage-categories — hydrates CategoryStore so the category/subcategory pickers reflect the current list
        // ac: configurable-currency-pair — hydrates CurrencySettingsStore for the ledger tab decision
        viewModelScope.launch {
            getAccounts().collect {
                AccountStore.hydrate(it)
                useFirstWalletIfNone()
            }
        }
        viewModelScope.launch { getCategories().collect(CategoryStore::hydrate) }
        viewModelScope.launch { getCurrencySettings().collect(CurrencySettingsStore::hydrate) }
        viewModelScope.launch { getAccounts.refresh() }
        viewModelScope.launch { getCategories.refresh() }
        viewModelScope.launch { getBudgetLines().onSuccess { _budgetLines.value = it } }
        viewModelScope.launch {
            val expense = getCategories().first().ifEmpty { DEFAULT_CATEGORIES }.map { it.name }
            _frequent.value = getFrequent.byType(mapOf("expense" to expense, "income" to INCOME_CATEGORIES, "investment" to INVESTMENT_CATEGORIES))
        }
        // ac: currency-books — a form nobody has typed in yet takes its wallet from the book
        viewModelScope.launch {
            BookStore.book.collect { book ->
                val f = _state.value.form
                if (f.amount.isEmpty() && f.description.isEmpty()) {
                    _state.value = _state.value.copy(form = f.copy(wallet = defaultWallet(book, f.wallet), toWallet = null))
                }
            }
        }
    }

    // ac: currency-books — the first account of the book's currency. COMBINED keeps the current wallet.
    private fun defaultWallet(book: Book, current: Account): Account {
        val code = book.currencyCode()
        val usable = current.id.isNotEmpty() && AccountStore.all.any { it.id == current.id }
        if (code == null) return if (usable) current else AccountStore.all.firstOrNull() ?: current
        return if (usable && current.currency == code) current else AccountStore.all.firstOrNull { it.currency == code } ?: current
    }

    // A new install has no accounts when the form is created. The first one loaded becomes the wallet.
    private fun useFirstWalletIfNone() {
        val f = _state.value.form
        if (f.wallet.id.isEmpty()) _state.value = _state.value.copy(form = f.copy(wallet = defaultWallet(BookStore.book.value, f.wallet)))
    }

    fun updateForm(form: TxFormValues) {
        val typedNew = form.description != _state.value.form.description
        _state.value = _state.value.copy(form = form, errorMessage = null, validationError = null)
        if (typedNew) requestSuggestion(form)
    }

    // Waits for a pause in typing, so the model runs once per description and not once per letter.
    private fun requestSuggestion(form: TxFormValues) {
        suggestJob?.cancel()
        _suggestion.value = null
        if (form.txType != TxType.EXPENSE) return
        suggestJob = viewModelScope.launch {
            delay(SUGGEST_DELAY_MS)
            _suggestion.value = suggestCategory(form.description)
        }
    }

    fun applySuggestion() {
        val s = _suggestion.value ?: return
        _suggestion.value = null
        _state.value = _state.value.copy(form = _state.value.form.copy(category = s.category, subcategory = s.subcategory))
    }

    // Clear the form back to defaults so the next time the add bottom sheet opens it starts fresh.
    fun reset() {
        suggestJob?.cancel()
        _suggestion.value = null
        val fresh = AddFormState()
        _state.value = fresh.copy(form = fresh.form.copy(wallet = defaultWallet(BookStore.book.value, fresh.form.wallet)))
    }

    fun submit() = viewModelScope.launch {
        val s = _state.value
        val f = s.form
        f.validate()?.let { _state.value = s.copy(validationError = it); return@launch }
        val amount = f.amount.toDouble()
        _state.value = s.copy(isLoading = true, errorMessage = null)

        // ac: configurable-currency-pair - the ledger tab is chosen against the configured home currency
        val tab = ledgerTabFor(f.wallet.currency, CurrencySettingsStore.current.homeCode, isIncome = f.txType == TxType.INCOME)
        // ac: custom-rules - a rule picks the category when the user left the default one. A typed description stays.
        val ruled = if (f.category == "Other" && f.subcategory == null) {
            applyRules(RuleInput(f.description, amount, f.txType.id, f.wallet.id, LocalDateTime.of(f.date, LocalTime.now()))).takeIf { it.category != null }
        } else null
        // ac: add-transaction-date — the submitted transaction uses the selected date, not the current date
        val entry = TransactionEntry(
            tab = tab,
            date = f.date,
            item = f.description,
            amount = amount,
            category = ruled?.category ?: f.category,
            wallet = f.wallet.id,
            txType = f.txType.id,
            toWallet = if (f.txType == TxType.TRANSFER) f.toWallet?.id else null,
            // ac: add-transfer-target-amount — store the received amount only for cross-currency transfers
            toAmount = if (f.isCrossCurrencyTransfer) f.toAmount.toDoubleOrNull() else null,
            // ac: add-transaction-subcategory — the submitted transaction stores the chosen subcategory
            subcategory = if (ruled != null) ruled.subcategory else f.subcategory,
        )
        val result = transactionRepository.insertManual(entry)
        _state.value = _state.value.copy(
            isLoading = false,
            // The row is saved on the phone before it syncs, so the form closes on a sync failure too.
            // The row shows Failed with Retry in the list. A second tap on Save would add a duplicate.
            successMessage = if (result.isSuccess) "Saved and syncing!" else "Saved offline. Sync failed.",
            errorMessage = null,
        )
    }
}
