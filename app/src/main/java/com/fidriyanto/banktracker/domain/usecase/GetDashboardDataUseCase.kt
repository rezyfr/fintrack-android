package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.repository.DashboardRepository
import com.fidriyanto.banktracker.data.repository.TransactionRepository
import com.fidriyanto.banktracker.domain.model.CategoryRow
import com.fidriyanto.banktracker.domain.model.DashboardSummaryPair
import com.fidriyanto.banktracker.domain.model.Period
import com.fidriyanto.banktracker.domain.model.TransactionUiModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
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
        // ac: insights-subcategory-breakdown — fetch the period's raw transactions once to build the
        // per-category subcategory breakdown, then merge it onto the overview-based summaries.
        return flow {
            val (thbSub, idrSub) = fetchSubcategoryBreakdowns(toYMPrefixes(months))
            // ac: transport-provider-breakdown — card updates when the period filter changes
            emitAll(
                combine(
                    repository.observeForMonths(months),
                    repository.observeTransportMerchants()
                ) { rows, transport ->
                    DashboardSummaryPair(
                        thb = buildSummary(rows.first, transport.first).copy(subcategoryBreakdown = thbSub),
                        idr = buildSummary(rows.second, transport.second).copy(subcategoryBreakdown = idrSub)
                    )
                }
            )
        }
    }

    // ac: insights-subcategory-breakdown — split the period's transactions by wallet currency and
    // build a subcategory breakdown for each side.
    private suspend fun fetchSubcategoryBreakdowns(
        prefixes: List<String>,
    ): Pair<Map<String, List<CategoryRow>>, Map<String, List<CategoryRow>>> {
        val all = mutableListOf<TransactionUiModel>()
        for (ym in prefixes) {
            transactionRepository.fetch(ym, null, null).onSuccess { all.addAll(it) }
        }
        val thb = all.filter { walletCurrencyOf(it.wallet) == "THB" }
        val idr = all.filter { walletCurrencyOf(it.wallet) == "IDR" }
        return buildSubcategoryBreakdown(thb) to buildSubcategoryBreakdown(idr)
    }

    suspend fun refresh(period: Period, customFrom: String?, customTo: String?, wallet: String? = null): Result<Unit> {
        if (wallet != null) return Result.success(Unit)
        return repository.refresh(resolveMonths(period, customFrom, customTo))
    }
}
