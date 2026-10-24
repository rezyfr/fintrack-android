package com.fidriyanto.banktracker.ui.home

import kotlinx.coroutines.flow.drop
import com.fidriyanto.banktracker.domain.usecase.convertToHome
import com.fidriyanto.banktracker.ui.common.holdsWallet
import com.fidriyanto.banktracker.ui.common.currencyCode
import com.fidriyanto.banktracker.ui.common.BookLoad
import com.fidriyanto.banktracker.ui.common.BookStore
import com.fidriyanto.banktracker.ui.common.DataChanges
import com.fidriyanto.banktracker.ui.common.Book
import com.fidriyanto.banktracker.domain.usecase.GetInstallmentsUseCase
import com.fidriyanto.banktracker.domain.model.Installment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fidriyanto.banktracker.data.model.TransactionEdit
import com.fidriyanto.banktracker.data.repository.TransactionRepository
import com.fidriyanto.banktracker.domain.model.CardStatement
import com.fidriyanto.banktracker.domain.model.SpendCompare
import com.fidriyanto.banktracker.domain.model.TransactionUiModel
import com.fidriyanto.banktracker.domain.usecase.BudgetGlance
import com.fidriyanto.banktracker.domain.usecase.GetBudgetGlanceUseCase
import com.fidriyanto.banktracker.domain.usecase.GetCardStatementsUseCase
import com.fidriyanto.banktracker.domain.usecase.GetCurrencySettingsUseCase
import com.fidriyanto.banktracker.domain.usecase.GetExchangeRateUseCase
import com.fidriyanto.banktracker.domain.usecase.PayCycle
import com.fidriyanto.banktracker.domain.usecase.buildSpendCompare
import com.fidriyanto.banktracker.domain.usecase.moneyFlow
import com.fidriyanto.banktracker.ui.common.AccountStore
import com.fidriyanto.banktracker.ui.common.CurrencySettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val loading: Boolean = true,
    val cycleFromIso: String = "",
    val cycleToIso: String = "",
    val daysToPayday: Int = 0,
    // ac: currency-books - the book shown, and its money in and out. COMBINED counts every currency in the
    // home currency.
    val book: Book = Book.COMBINED,
    val moneyIn: Double = 0.0,
    val moneyOut: Double = 0.0,
    val spendCurrency: String = "",
    val ccPayments: Double = 0.0,
    val cards: List<CardStatement> = emptyList(),
    val recent: List<TransactionUiModel> = emptyList(),
    val budget: BudgetGlance? = null,
    val spend: SpendCompare? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val getCardStatements: GetCardStatementsUseCase,
    private val getBudgetGlance: GetBudgetGlanceUseCase,
    private val getCurrencySettings: GetCurrencySettingsUseCase,
    private val getInstallments: GetInstallmentsUseCase,
    private val getExchangeRate: GetExchangeRateUseCase,
) : ViewModel() {
    // ac: installment-link-edit — the plans shown in the edit sheet. They load when the sheet opens.
    private val _installments = kotlinx.coroutines.flow.MutableStateFlow<List<Installment>>(emptyList())
    val installments: kotlinx.coroutines.flow.StateFlow<List<Installment>> = _installments

    fun loadInstallments() {
        viewModelScope.launch { getInstallments().onSuccess { _installments.value = it } }
    }

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        // ac: book-switch-transition — the book switch waits for this page to finish loading before it reveals it
        viewModelScope.launch { state.collect { BookLoad.report(BookLoad.HOME, it.loading) } }
        // ac: configurable-currency-pair — hydrates CurrencySettingsStore so the hero reflects the
        // configured pair instead of hardcoded "Thailand"/"Indonesia"
        viewModelScope.launch { getCurrencySettings().collect(CurrencySettingsStore::hydrate) }
        load()
        // ac: currency-books — a new book reloads the overview for it
        viewModelScope.launch { BookStore.book.drop(1).collect { load() } }
        // A transaction saved from the Add sheet changes the totals, so the overview reloads.
        viewModelScope.launch { DataChanges.count.drop(1).collect { load() } }
    }

    fun load() {
        _state.update { it.copy(loading = true) }
        viewModelScope.launch {
            val book = BookStore.book.value
            val cycle = PayCycle.current()
            val rows = transactionRepository
                .fetch(month = null, wallet = null, txType = null, dateFrom = cycle.fromIso, dateTo = cycle.toIso)
                .getOrElse { emptyList() }
            // ac: configurable-currency-pair - a wallet's zone comes from its own account currency
            val homeCode = CurrencySettingsStore.current.homeCode
            // ac: home-cycle-overview - money in/out per zone = income/expense plus cross-zone transfers
            // (destination gets the received amount). Within-zone transfers cancel. Investments are a
            // separate zone (an investment withdrawal into a home-currency account is home-currency in).
            fun curOf(w: String?) = if (AccountStore.isInvestment(w)) "INV" else AccountStore.all.firstOrNull { it.id == w }?.currency ?: homeCode
            // ac: exchange-rate-from-transfers - each rate comes from the user's own recent transfers
            val rates = getExchangeRate.rates()
            // ac: currency-books - COMBINED counts every currency as one zone in the home currency, so a
            // transfer between two currencies is neither money in nor money out
            val flow = book.code?.let { moneyFlow(rows, it, ::curOf) }
                ?: moneyFlow(convertToHome(rows, rates, ::curOf), "ALL") { w -> if (AccountStore.isInvestment(w)) "INV" else "ALL" }
            // ac: card-due-reflects-payments — a fully paid statement is not "due", so drop it from Home
            val statements = getCardStatements.getStatements().getOrElse { emptyList() }
            val cards = statements.filter { it.balance > 0.0 && book.holdsWallet(it.wallet) }
            // ac: home-cycle-overview — total card payments (cash -> card wallet); separate hero info only.
            // The card wallets are the ones with a billing record, not a hardcoded list.
            val cardWallets = statements.map { it.wallet }.toSet()
            val ccPayments = rows.sumOf { r ->
                if (r.txType == "transfer" && r.toWallet in cardWallets && book.holdsWallet(r.toWallet)) r.amount else 0.0
            }
            val recent = rows
                .filter { (it.txType == "income" || it.txType == "expense") && book.holdsWallet(it.wallet) }
                .sortedByDescending { it.dateIso }
                .take(5)
            val budget = getBudgetGlance.getGlance(book.currencyCode()).getOrNull()
            // ac: home-cycle-spending-comparison — load the previous cycle too. "Spending" = home-currency
            // account expenses plus foreign-currency account expenses converted to home currency; Investments excluded.
            val prevCycle = PayCycle.cycleRange(1)
            val prevRows = transactionRepository
                .fetch(month = null, wallet = null, txType = null, dateFrom = prevCycle.fromIso, dateTo = prevCycle.toIso)
                .getOrElse { emptyList() }
            // ac: currency-books - one book counts only its own currency; COMBINED converts every currency
            val spendHome: (TransactionUiModel) -> Double = { r ->
                val cur = curOf(r.wallet)
                when {
                    r.txType != "expense" || cur == "INV" -> 0.0
                    book.isCombined -> rates.toHome(cur, r.amount)
                    cur == book.code -> r.amount
                    else -> 0.0
                }
            }
            // ac: spend-compare-more-cycles — two more past cycles for the trend view
            val olderCycles = (2..3).map { PayCycle.cycleRange(it) }.map { range ->
                range to transactionRepository
                    .fetch(month = null, wallet = null, txType = null, dateFrom = range.fromIso, dateTo = range.toIso)
                    .getOrElse { emptyList() }
            }
            val spend = buildSpendCompare(cycle, prevCycle, rows, prevRows, olderCycles, spendHome)
            _state.value = HomeUiState(
                loading = false,
                cycleFromIso = cycle.fromIso,
                cycleToIso = cycle.toIso,
                daysToPayday = PayCycle.daysToPayday(),
                book = book,
                moneyIn = flow.moneyIn,
                moneyOut = flow.moneyOut,
                spendCurrency = book.currencyCode() ?: homeCode,
                ccPayments = ccPayments,
                cards = cards,
                recent = recent,
                budget = budget,
                spend = spend,
            )
        }
    }

    fun edit(id: Long, edit: TransactionEdit) = viewModelScope.launch {
        // ac: edit-transaction-from-feed — failure path emits an event so the snackbar can offer Retry that re-attempts the PATCH
        val result = transactionRepository.editTransaction(id, edit)
        result.onSuccess {
            _state.update {
                it.copy(
                    recent = it.recent.toMutableList().map { tx ->
                        if (tx.id == id) tx.copy(item = edit.item, category = edit.category, amount = edit.amount, dateIso = edit.dateIso, wallet = edit.wallet, txType = edit.txType, subcategory = edit.subcategory, toWallet = edit.toWallet, toAmount = edit.toAmount) else tx
                    }
                )
            }
        }
    }

    override fun onCleared() {
        BookLoad.report(BookLoad.HOME, false)
        super.onCleared()
    }
}
