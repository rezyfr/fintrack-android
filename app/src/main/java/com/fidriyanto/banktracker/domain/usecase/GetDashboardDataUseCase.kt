package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.repository.DashboardRepository
import com.fidriyanto.banktracker.data.repository.TransactionRepository
import com.fidriyanto.banktracker.domain.model.DashboardSummaryPair
import com.fidriyanto.banktracker.domain.model.Period
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetDashboardDataUseCase @Inject constructor(
    private val repository: DashboardRepository,
    private val transactionRepository: TransactionRepository,
) {
    // ac: insights-filter-by-wallet — wallet parameter filters data for a specific wallet
    fun observe(period: Period, customFrom: String?, customTo: String?, wallet: String? = null): Flow<DashboardSummaryPair> {
        val months = resolveMonths(period, customFrom, customTo)
        if (wallet != null) return observeByWallet(transactionRepository, toYMPrefixes(months), wallet)
        val (fromDate, toDate) = monthsToDateRange(months)
        return combine(
            repository.observeForMonths(months),
            repository.observeTransportBreakdown(fromDate, toDate)
        ) { rows, transport ->
            DashboardSummaryPair(
                thb = buildSummary(rows.first, transport.first),
                idr = buildSummary(rows.second, transport.second)
            )
        }
    }

    suspend fun refresh(period: Period, customFrom: String?, customTo: String?, wallet: String? = null): Result<Unit> {
        if (wallet != null) return Result.success(Unit)
        return repository.refresh(resolveMonths(period, customFrom, customTo))
    }
}
