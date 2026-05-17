package com.fidriyanto.banktracker.fake

import com.fidriyanto.banktracker.data.db.MonthlyOverviewEntity
import com.fidriyanto.banktracker.sheets.MonthlyOverviewFetcher
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeMonthlyOverviewFetcher @Inject constructor() : MonthlyOverviewFetcher {
    var shouldSucceed = true
    var fetchCalled = false
    var stubbedRows: List<MonthlyOverviewEntity> = emptyList()

    override suspend fun fetch(): Result<MonthlyOverviewFetcher.FetchResult> {
        fetchCalled = true
        return if (shouldSucceed) {
            Result.success(MonthlyOverviewFetcher.FetchResult(rows = stubbedRows, budgets = emptyList()))
        } else {
            Result.failure(Exception("Fake fetch error"))
        }
    }

    fun reset() {
        shouldSucceed = true
        fetchCalled = false
        stubbedRows = emptyList()
    }
}
