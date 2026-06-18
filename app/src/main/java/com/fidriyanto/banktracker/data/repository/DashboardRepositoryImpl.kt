package com.fidriyanto.banktracker.data.repository

import com.fidriyanto.banktracker.data.datasource.MonthlyOverviewLocalDataSource
import com.fidriyanto.banktracker.data.datasource.MonthlyOverviewRemoteDataSource
import com.fidriyanto.banktracker.data.db.MonthlyOverviewEntity
import com.fidriyanto.banktracker.domain.model.MerchantTotal
import com.fidriyanto.banktracker.domain.model.MonthlyOverviewSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DashboardRepositoryImpl @Inject constructor(
    private val localDataSource: MonthlyOverviewLocalDataSource,
    private val remoteDataSource: MonthlyOverviewRemoteDataSource
) : DashboardRepository {

    // in-memory cache populated during refresh(); survives for the session
    private val _thbMerchants = MutableStateFlow<List<MerchantTotal>>(emptyList())
    private val _idrMerchants = MutableStateFlow<List<MerchantTotal>>(emptyList())

    override fun observeForMonths(
        months: List<String>
    ): Flow<Pair<List<MonthlyOverviewSummary>, List<MonthlyOverviewSummary>>> =
        combine(
            localDataSource.observeByMonths(months, "THB").map { it.map(::toSummary) },
            localDataSource.observeByMonths(months, "IDR").map { it.map(::toSummary) }
        ) { thb, idr -> Pair(thb, idr) }

    // ac: transport-provider-breakdown — THB and IDR transport breakdowns are shown independently
    override fun observeTransportMerchants(): Flow<Pair<List<MerchantTotal>, List<MerchantTotal>>> =
        combine(_thbMerchants, _idrMerchants) { thb, idr -> Pair(thb, idr) }

    override suspend fun refresh(months: List<String>): Result<Unit> {
        return try {
            val result = remoteDataSource.fetch(months)
            result.onSuccess { rows -> localDataSource.upsertAll(rows) }
            // fetch transport merchants on every refresh; failure is non-fatal
            remoteDataSource.fetchTransportMerchants(months).onSuccess { (thb, idr) ->
                _thbMerchants.value = thb
                _idrMerchants.value = idr
            }
            result.map { }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun toSummary(e: MonthlyOverviewEntity) = MonthlyOverviewSummary(
        income = e.income, totalExpenditure = e.totalExpenditure,
        bills = e.bills, subscriptions = e.subscriptions, entertainment = e.entertainment,
        foodDrink = e.foodDrink, groceries = e.groceries, healthWellbeing = e.healthWellbeing,
        family = e.family, other = e.other, shopping = e.shopping,
        transport = e.transport, travel = e.travel, business = e.business, gifts = e.gifts
    )
}
