package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.repository.AccountRepository
import com.fidriyanto.banktracker.data.repository.CurrencySettingsRepository
import com.fidriyanto.banktracker.data.repository.TransactionRepository
import com.fidriyanto.banktracker.domain.model.AccountType
import com.fidriyanto.banktracker.domain.model.CurrencySummary
import com.fidriyanto.banktracker.domain.model.DashboardSummaries
import com.fidriyanto.banktracker.domain.model.TransactionUiModel
import com.fidriyanto.banktracker.domain.model.currenciesInUse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

private const val COMBINED_ZONE = "ALL"
private val EMPTY_SUMMARY = CurrencySummary(0.0, 0.0, 0.0, emptyList())

@Singleton
class GetDashboardDataUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val currencySettingsRepository: CurrencySettingsRepository,
) {
    // ac: insights-cycle-stepper: cycleOffset picks one pay cycle (0 is the cycle in progress, 1 the one before it).
    fun observe(cycleOffset: Int, wallet: String? = null): Flow<DashboardSummaries> = flow {
        val accounts = accountRepository.observeAll().first()
        val currencyByWallet = accounts.associate { it.id to it.currency }
        val investmentWallets = accounts.filter { it.type == AccountType.INVESTMENT }.map { it.id }.toSet()
        val settings = currencySettingsRepository.observe().first()
        val homeCode = settings.homeCode
        PayCycle.startDay = settings.cycleStartDay
        val cycles = trendCycles(cycleOffset)
        val cycle = cycles.last()
        val prev = PayCycle.cycleRange(cycleOffset + 1)
        val fetched = transactionRepository.fetchRanges((cycles + prev).map { it.fromIso to it.toIso }, wallet)
        fun rowsOf(c: PayCycle.Range) = fetched.getValue(c.fromIso to c.toIso)
        val rows = rowsOf(cycle)
        val prevRows = rowsOf(prev)
        val cycleRows = cycles.map(::rowsOf)
        // ac: insights-wallet-flow - one wallet is its own zone, so every transfer into or out of it
        // counts, also between two wallets of the same currency. The other currencies stay empty.
        if (wallet != null) {
            val walletZoneOf: (String?) -> String = { w -> if (w == wallet) wallet else "OTHER" }
            val own: (TransactionUiModel) -> Boolean = { it.wallet == wallet }
            val summary = buildSummaryFromTransactions(rows.filter(own), moneyFlow(rows, wallet, walletZoneOf), prevRows.filter(own))
                .copy(monthlyTrend = buildMonthlyTrend(cycles, cycleRows, wallet, walletZoneOf))
            emit(DashboardSummaries(mapOf(walletCurrencyOf(wallet, currencyByWallet, homeCode) to summary), EMPTY_SUMMARY, cycle.fromIso, cycle.toIso))
            return@flow
        }
        // A wallet's zone is its currency. Investments are a zone of their own, so moving cash into or
        // out of them counts as money out or in, the same as on Home.
        val currencyOf: (String?) -> String = { walletCurrencyOf(it, currencyByWallet, homeCode) }
        val zoneOf: (String?) -> String = { w -> if (w in investmentWallets) "INV" else currencyOf(w) }
        // The flow reads every row, because a transfer between two currencies belongs to both sections.
        fun summaryFor(zone: String): CurrencySummary =
            buildSummaryFromTransactions(rows.filter { currencyOf(it.wallet) == zone }, moneyFlow(rows, zone, zoneOf), prevRows.filter { currencyOf(it.wallet) == zone })
                .copy(monthlyTrend = buildMonthlyTrend(cycles, cycleRows, zone, zoneOf))
        // ac: insights-combined-currency - all currencies in the home currency, as one zone, so a
        // transfer between two currencies is neither money in nor money out
        val combinedZoneOf: (String?) -> String = { w -> if (w in investmentWallets) "INV" else COMBINED_ZONE }
        val rates = transactionRepository.rateTable(currencyByWallet, settings)
        fun convert(list: List<TransactionUiModel>) = convertToHome(list, rates, currencyOf)
        val combinedRows = convert(rows)
        val combined = buildSummaryFromTransactions(combinedRows, moneyFlow(combinedRows, COMBINED_ZONE, combinedZoneOf), convert(prevRows))
            .copy(monthlyTrend = buildMonthlyTrend(cycles, cycleRows.map(::convert), COMBINED_ZONE, combinedZoneOf))
        emit(DashboardSummaries(currenciesInUse(homeCode, accounts).associateWith(::summaryFor), combined, cycle.fromIso, cycle.toIso))
    }
}
