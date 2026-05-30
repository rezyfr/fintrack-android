package com.fidriyanto.banktracker.data.repository

import com.fidriyanto.banktracker.data.datasource.MonthlyOverviewLocalDataSource
import com.fidriyanto.banktracker.data.datasource.MonthlyOverviewRemoteDataSource
import com.fidriyanto.banktracker.data.datasource.TransactionLocalDataSource
import com.fidriyanto.banktracker.data.db.MerchantTotal
import com.fidriyanto.banktracker.data.db.MonthlyOverviewEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DashboardRepositoryImpl @Inject constructor(
    private val localDataSource: MonthlyOverviewLocalDataSource,
    private val remoteDataSource: MonthlyOverviewRemoteDataSource,
    private val transactionDataSource: TransactionLocalDataSource
) : DashboardRepository {

    override fun observeForMonths(
        months: List<String>
    ): Flow<Pair<List<MonthlyOverviewEntity>, List<MonthlyOverviewEntity>>> =
        combine(
            localDataSource.observeByMonths(months, "THB"),
            localDataSource.observeByMonths(months, "IDR")
        ) { thb, idr -> Pair(thb, idr) }

    override fun observeTransportBreakdown(
        fromDate: String,
        toDate: String
    ): Flow<Pair<List<MerchantTotal>, List<MerchantTotal>>> =
        combine(
            transactionDataSource.observeTransportTotalsTHB("Transport", fromDate, toDate),
            transactionDataSource.observeTransportTotalsIDR("Transport", fromDate, toDate)
        ) { thb, idr -> Pair(thb, idr) }

    override suspend fun refresh(months: List<String>): Result<Unit> {
        return try {
            val result = remoteDataSource.fetch(months)
            result.onSuccess { rows -> localDataSource.upsertAll(rows) }
            result.map { }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
