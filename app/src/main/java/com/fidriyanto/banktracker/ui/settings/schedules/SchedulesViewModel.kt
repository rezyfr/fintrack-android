package com.fidriyanto.banktracker.ui.settings.schedules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fidriyanto.banktracker.domain.model.Account
import com.fidriyanto.banktracker.domain.model.Schedule
import com.fidriyanto.banktracker.domain.model.UpcomingPayment
import com.fidriyanto.banktracker.domain.model.upcomingPayments
import com.fidriyanto.banktracker.domain.usecase.GetAccountsUseCase
import com.fidriyanto.banktracker.domain.usecase.ManageSchedulesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

const val UPCOMING_DAYS = 30

data class SchedulesState(
    val schedules: List<Schedule> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val upcoming: List<UpcomingPayment> = emptyList(),
    // What goes out in the next 30 days, by currency. Income and transfers are not counted.
    val dueByCurrency: Map<String, Double> = emptyMap(),
)

@HiltViewModel
class SchedulesViewModel @Inject constructor(
    private val manage: ManageSchedulesUseCase,
    getAccounts: GetAccountsUseCase,
) : ViewModel() {
    val state: StateFlow<SchedulesState> = combine(manage.observe(), getAccounts()) { schedules, accounts ->
        val upcoming = upcomingPayments(schedules, LocalDate.now(), UPCOMING_DAYS)
        val currencyOf = accounts.associate { it.id to it.currency }
        val due = upcoming.filter { it.schedule.txType == "expense" }
            .groupBy { currencyOf[it.schedule.walletId].orEmpty() }
            .mapValues { (_, list) -> list.sumOf { it.schedule.amount } }
        SchedulesState(schedules, accounts, upcoming, due)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SchedulesState())

    fun setAutoPost(id: String, on: Boolean) = viewModelScope.launch { manage.setAutoPost(id, on) }
}
