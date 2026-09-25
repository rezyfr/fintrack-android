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
    val thbIn: Double = 0.0,
    val thbOut: Double = 0.0,
    val idrIn: Double = 0.0,
    val idrOut: Double = 0.0,
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

    init { load() }

    fun load() {
        _state.value = HomeUiState(loading = true)
        viewModelScope.launch {
            val cycle = PayCycle.current()
            val rows = transactionRepository
                .fetch(month = null, wallet = null, txType = null, dateFrom = cycle.fromIso, dateTo = cycle.toIso)
                .getOrElse { emptyList() }
            // ac: home-cycle-overview — money in/out per zone = income/expense plus cross-zone transfers
            // (received amount for the destination). Within-zone transfers cancel out and are ignored.
            // Zones: THB (Bangkok Bank), IDR (spending accounts), and Investments as a separate pool —
            // so withdrawing from Investments into BCA counts as money in on the IDR side, and a THB
            // salary moved to IDR leaves THB ~0 and lands on IDR.
            fun curOf(w: String?) = when (w) {
                "BBL" -> "THB"
                "INVESTMENT" -> "INV"
                else -> "IDR"
            }
            fun moneyIn(cur: String) = rows.filter { it.wallet != null }.sumOf { r ->
                when {
                    r.txType == "income" && curOf(r.wallet) == cur -> r.amount
                    r.txType == "transfer" && r.toWallet != null && curOf(r.toWallet) == cur && curOf(r.wallet) != cur -> r.toAmount ?: r.amount
                    else -> 0.0
                }
            }
            fun moneyOut(cur: String) = rows.filter { it.wallet != null }.sumOf { r ->
                when {
                    r.txType == "expense" && curOf(r.wallet) == cur -> r.amount
                    r.txType == "transfer" && r.toWallet != null && curOf(r.wallet) == cur && curOf(r.toWallet) != cur -> r.amount
                    else -> 0.0
                }
            }
            val thbIn = moneyIn("THB")
            val thbOut = moneyOut("THB")
            val idrIn = moneyIn("IDR")
            val idrOut = moneyOut("IDR")
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
                thbIn = thbIn,
                thbOut = thbOut,
                idrIn = idrIn,
                idrOut = idrOut,
                cards = cards,
                recent = recent,
                budget = budget,
            )
        }
    }
}
