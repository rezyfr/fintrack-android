package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.repository.AccountRepository
import com.fidriyanto.banktracker.data.repository.WalletBalanceRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class GetGrowthHistoryUseCase @Inject constructor(
    private val accounts: AccountRepository,
    private val balances: WalletBalanceRepository,
) {
    // ac: account-growth-chart — the balance of every account at the end of each of the last cycles,
    // and today. One request carries all the dates, so the ranges are cut on the device.
    suspend operator fun invoke(today: LocalDate = LocalDate.now()): Result<GrowthHistory> = runCatching {
        val dates = cycleEnds(today) + today
        val rows = balances.getBalancesAt(dates.map { it.format(DateTimeFormatter.ISO_LOCAL_DATE) }).getOrThrow()
        val byAccountAndDate = rows.associate { (it.walletId to it.asOf) to it.balance }
        val list = accounts.observeAll().first().map { account ->
            AccountHistory(
                accountId = account.id,
                currency = account.currency,
                type = account.type,
                values = dates.map { byAccountAndDate[account.id to it.format(DateTimeFormatter.ISO_LOCAL_DATE)] ?: 0.0 },
            )
        }
        GrowthHistory(dates, list)
    }

    internal companion object {
        // Five years of cycles. The oldest are cut when nothing happened in them.
        const val MAX_CYCLES = 60

        // The last day of each finished pay cycle, oldest first. The cycle in progress has not ended.
        fun cycleEnds(today: LocalDate, startDay: Int = PayCycle.startDay): List<LocalDate> =
            (MAX_CYCLES downTo 1).map { PayCycle.cycleRange(it, today, startDay).end }
    }
}
