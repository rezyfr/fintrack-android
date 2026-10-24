package com.fidriyanto.banktracker.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fidriyanto.banktracker.domain.usecase.GetAccountsUseCase
import com.fidriyanto.banktracker.domain.usecase.GetCurrencySettingsUseCase
import com.fidriyanto.banktracker.domain.usecase.GetDashboardDataUseCase
import com.fidriyanto.banktracker.ui.common.AccountStore
import com.fidriyanto.banktracker.ui.common.BookStore
import com.fidriyanto.banktracker.ui.common.CurrencySettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val useCase: GetDashboardDataUseCase,
    private val getCurrencySettings: GetCurrencySettingsUseCase,
    private val getAccounts: GetAccountsUseCase,
) : ViewModel() {
    // ac: insights-cycle-stepper — 0 is the pay cycle in progress, 1 the one before it, and so on
    private val _cycleOffset = MutableStateFlow(0)
    // ac: insights-filter-by-wallet — wallet filter state
    private val _walletFilter = MutableStateFlow<String?>(null)
    private val _isRefreshing = MutableStateFlow(false)
    // incremented on every refresh() so the cold flow re-runs, since the data is not cached in Room
    private val _refreshTick = MutableStateFlow(0)

    val walletFilter = _walletFilter.asStateFlow()

    val state: StateFlow<DashboardUiState> =
        combine(_cycleOffset, _walletFilter, _refreshTick) { offset, w, _ -> offset to w }
            .flatMapLatest { (offset, w) ->
                combine(
                    useCase.observe(offset, w).onEach { _isRefreshing.value = false },
                    _isRefreshing,
                ) { summaries, refreshing ->
                    DashboardUiState.Loaded(
                        cycleOffset = offset,
                        byCurrency = summaries.byCurrency,
                        combined = summaries.combined,
                        fromIso = summaries.fromIso,
                        toIso = summaries.toIso,
                        isRefreshing = refreshing,
                    )
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState.LoadingNoCache)

    init {
        // ac: configurable-currency-pair — hydrates CurrencySettingsStore for the currency toggle
        viewModelScope.launch { getCurrencySettings().collect(CurrencySettingsStore::hydrate) }
        // ac: insights-wallet-flow — the wallet chips list the accounts table
        viewModelScope.launch { getAccounts().collect(AccountStore::hydrate) }
        // ac: currency-books — the wallet chips belong to one book, so a new book clears the wallet
        viewModelScope.launch { BookStore.book.drop(1).collect { _walletFilter.value = null } }
    }

    // ac: insights-cycle-stepper — step one pay cycle back (+1) or forward (-1). The cycle in progress
    // is the latest one.
    fun stepCycle(delta: Int) {
        _cycleOffset.value = (_cycleOffset.value + delta).coerceAtLeast(0)
    }

    // ac: insights-filter-by-wallet — wallet filter persists when the cycle is changed
    fun setWallet(wallet: String?) {
        _walletFilter.value = wallet
    }

    // Re-runs observe(), which fetches fresh transactions. The next emission clears isRefreshing.
    fun refresh() {
        _isRefreshing.value = true
        _refreshTick.value++
    }
}
