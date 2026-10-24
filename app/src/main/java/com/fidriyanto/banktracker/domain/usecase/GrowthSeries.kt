package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.domain.model.AccountType
import java.time.LocalDate

// ac: account-growth-chart — the balance of each account at the end of each pay cycle, and one more
// point for today. The last date is always today, so the last point is a cycle still in progress.
data class AccountHistory(val accountId: String, val currency: String, val type: AccountType, val values: List<Double>)

data class GrowthHistory(val dates: List<LocalDate>, val accounts: List<AccountHistory>)

data class GrowthPoint(val date: LocalDate, val checking: Double, val investment: Double, val isNow: Boolean) {
    val total: Double get() = checking + investment
}

data class GrowthSeries(val currency: String, val points: List<GrowthPoint>)

// How many points a range shows, counting today. A null count is all the history there is.
enum class GrowthRange(val points: Int?) { SIX_MONTHS(6), TWELVE_MONTHS(12), ALL(null) }

object GrowthSeriesBuilder {
    // The series for one currency. A credit card is left out, as it is in the net worth. Checking is
    // every account that is not investment. Returns null when there is nothing to draw: no account
    // in the currency, no activity at all, or fewer than two points.
    fun build(
        history: GrowthHistory,
        currency: String,
        range: GrowthRange,
        include: (accountId: String) -> Boolean = { true },
        includeInvestment: Boolean = true,
    ): GrowthSeries? {
        val accounts = history.accounts.filter {
            it.currency == currency && counts(it, include, includeInvestment)
        }
        if (accounts.isEmpty() || history.dates.isEmpty()) return null

        // Before an account's first transaction its balance does not move. The history reaches back
        // five years whether or not there is data, so the leading run of unchanged points is cut,
        // keeping the last of them as the starting level.
        val firstChange = history.dates.indices.drop(1).firstOrNull { i -> accounts.any { it.values[i] != it.values[0] } }
            ?: return null
        val start = firstChange - 1

        var indices = (start until history.dates.size).toList()
        range.points?.let { indices = indices.takeLast(it) }
        if (indices.size < 2) return null

        val last = history.dates.size - 1
        val points = indices.map { i ->
            GrowthPoint(
                date = history.dates[i],
                checking = accounts.filter { it.type != AccountType.INVESTMENT }.sumOf { it.values[i] },
                investment = accounts.filter { it.type == AccountType.INVESTMENT }.sumOf { it.values[i] },
                isNow = i == last,
            )
        }
        return GrowthSeries(currency, points)
    }

    // ac: account-growth-toggle — with the investment switched off, investment accounts are left out of
    // the bands and of the total, so the figure is what is held outside investment.
    private fun counts(account: AccountHistory, include: (String) -> Boolean, includeInvestment: Boolean): Boolean =
        account.type != AccountType.CREDIT && include(account.accountId) && (includeInvestment || account.type != AccountType.INVESTMENT)

    // The currencies that have something to draw, so the card can offer only those.
    fun currencies(
        history: GrowthHistory,
        include: (accountId: String) -> Boolean = { true },
        includeInvestment: Boolean = true,
    ): List<String> = history.accounts.filter { counts(it, include, includeInvestment) }.map { it.currency }.distinct()

    // Whether there is an investment account to switch off, so the card shows the switch only then.
    fun hasInvestment(history: GrowthHistory, include: (accountId: String) -> Boolean = { true }): Boolean =
        history.accounts.any { it.type == AccountType.INVESTMENT && include(it.accountId) }
}
