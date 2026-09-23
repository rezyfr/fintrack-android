package com.fidriyanto.banktracker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fidriyanto.banktracker.data.repository.TransactionRepository
import com.fidriyanto.banktracker.domain.model.CardStatement
import com.fidriyanto.banktracker.domain.model.TransactionUiModel
import com.fidriyanto.banktracker.domain.usecase.BudgetGlance
import com.fidriyanto.banktracker.domain.usecase.GetBudgetGlanceUseCase
import com.fidriyanto.banktracker.domain.usecase.GetCardStatementsUseCase
import com.fidriyanto.banktracker.domain.usecase.PayCycle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val loading: Boolean = true,
    val cycleFromIso: String = "",
    val cycleToIso: String = "",
    val daysToPayday: Int = 0,
    val income: Double = 0.0,
    val expenses: Double = 0.0,
    val net: Double = 0.0,
    val cards: List<CardStatement> = emptyList(),
    val recent: List<TransactionUiModel> = emptyList(),
    val budget: BudgetGlance? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val getCardStatements: GetCardStatementsUseCase,
    private val getBudgetGlance: GetBudgetGlanceUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    // IDR is the home currency for the cycle summary; Bangkok Bank (THB) is excluded here.
    private val idrWallets = setOf("BCA", "BCA_CC", "MANDIRI", "MANDIRI_CC", "INVESTMENT")

    init { load() }

    fun load() {
        _state.value = HomeUiState(loading = true)
        viewModelScope.launch {
            val cycle = PayCycle.current()
            val rows = transactionRepository
                .fetch(month = null, wallet = null, txType = null, dateFrom = cycle.fromIso, dateTo = cycle.toIso)
                .getOrElse { emptyList() }
            // ac: home-cycle-overview — cycle income, spending and net (IDR, self-transfers excluded)
            val idr = rows.filter { it.wallet in idrWallets && (it.txType == "income" || it.txType == "expense") }
            val income = idr.filter { it.txType == "income" }.sumOf { it.amount }
            val expenses = idr.filter { it.txType == "expense" }.sumOf { it.amount }
            val recent = rows
                .filter { it.txType == "income" || it.txType == "expense" }
                .sortedByDescending { it.dateIso }
                .take(5)
            val cards = getCardStatements.getStatements().getOrElse { emptyList() }
            val budget = getBudgetGlance.getGlance("IDR").getOrNull()
            _state.value = HomeUiState(
                loading = false,
                cycleFromIso = cycle.fromIso,
                cycleToIso = cycle.toIso,
                daysToPayday = PayCycle.daysToPayday(),
                income = income,
                expenses = expenses,
                net = income - expenses,
                cards = cards,
                recent = recent,
                budget = budget,
            )
        }
    }
}
