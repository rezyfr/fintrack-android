package com.fidriyanto.banktracker.data.datasource

import android.util.Log
import com.fidriyanto.banktracker.data.datasource.remote.TransactionApiService
import com.fidriyanto.banktracker.data.datasource.remote.dto.TransactionDto
import com.fidriyanto.banktracker.data.datasource.remote.mapper.toEntity
import com.fidriyanto.banktracker.data.db.TransactionEntity
import javax.inject.Inject
import javax.inject.Singleton

private const val PAGE_SIZE = 200
private const val MAX_PAGES = 25

@Singleton
class TransactionFetchDataSourceImpl @Inject constructor(
    private val service: TransactionApiService
) : TransactionFetchDataSource {
    override suspend fun fetch(
        month: String?,
        wallet: String?,
        txType: String?,
        category: String?,
        search: String?,
        amountMin: Double?,
        amountMax: Double?,
        dateFrom: String?,
        dateTo: String?,
        involvingWallet: String?,
    ): Result<List<TransactionEntity>> = runCatching {
        // ac: advanced-transaction-filters — date range overrides month filter
        val hasRange = dateFrom != null || dateTo != null
        suspend fun page(offset: Int) = service.fetchTransactions(
            month = month.takeIf { !hasRange },
            dateFrom = dateFrom,
            dateTo = dateTo,
            wallet = wallet,
            involvingWallet = involvingWallet,
            txType = txType,
            category = category,
            search = search,
            amountMin = amountMin,
            amountMax = amountMax,
            limit = PAGE_SIZE,
            offset = offset.takeIf { it > 0 },
        )
        // ac: complete-range-fetch — a range bounded at both ends reads every page, so a cycle with
        // more than one page of rows is complete. An open-ended fetch keeps the first page only.
        val bounded = (dateFrom != null && dateTo != null) || (!hasRange && month != null)
        val dtos = ArrayList<TransactionDto>()
        var next: Int? = 0
        var pages = 0
        do {
            val result = page(next ?: 0)
            dtos.addAll(result.items)
            next = result.nextOffset
            pages++
        } while (bounded && next != null && pages < MAX_PAGES)
        Log.d("TransactionFetchDS", "fetched ${dtos.size} transactions")
        dtos.mapNotNull { it.toEntity() }
    }.onFailure { Log.e("TransactionFetchDS", "fetch failed", it) }

    override suspend fun recentTransferRates(fromWallets: List<String>, toWallets: List<String>, limit: Int): Result<List<Double>> = runCatching {
        if (fromWallets.isEmpty() || toWallets.isEmpty()) return@runCatching emptyList()
        service.fetchTransferRates(fromWallets.joinToString(","), toWallets.joinToString(","), limit).rates
    }
}
