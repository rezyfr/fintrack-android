package com.fidriyanto.banktracker.sheets

import okhttp3.OkHttpClient
import javax.inject.Inject

class MonthlyOverviewFetcherImpl @Inject constructor(
    private val httpClient: OkHttpClient
) : MonthlyOverviewFetcher {
    override suspend fun fetch(): Result<MonthlyOverviewFetcher.FetchResult> =
        Result.failure(Exception("Dashboard data not available — will be replaced in Sub-project 3"))
}
