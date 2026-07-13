package com.fidriyanto.banktracker.data.datasource

import android.util.Log
import com.fidriyanto.banktracker.data.datasource.remote.SupabaseTransactionService
import com.fidriyanto.banktracker.data.datasource.remote.mapper.toEntity
import com.fidriyanto.banktracker.data.db.TransactionEntity
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionFetchDataSourceImpl @Inject constructor(
    private val service: SupabaseTransactionService
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
    ): Result<List<TransactionEntity>> = runCatching {
        // ac: advanced-transaction-filters — date range overrides month filter
        val dateFilters = if (dateFrom != null || dateTo != null) {
            listOfNotNull(
                dateFrom?.let { "gte.$it" },
                dateTo?.let { "lte.$it" },
            ).ifEmpty { null }
        } else {
            month?.let {
                val (y, m) = it.split("-").map { p -> p.toInt() }
                val lastDay = LocalDate.of(y, m, 1).lengthOfMonth().toString().padStart(2, '0')
                listOf("gte.${it}-01", "lte.${it}-${lastDay}")
            }
        }
        // ac: advanced-transaction-filters — amount range
        val amountFilters = listOfNotNull(
            amountMin?.let { "gte.$it" },
            amountMax?.let { "lte.$it" },
        ).ifEmpty { null }
        val dtos = service.fetchTransactions(
            order         = "date.desc",
            limit         = 200,
            dateFilters   = dateFilters,
            wallet        = wallet?.let { "eq.$it" },
            txType        = txType?.let { "eq.$it" },
            category      = category?.let { "eq.$it" },
            itemFilter    = search?.let { "ilike.*$it*" },
            amountFilters = amountFilters,
        )
        Log.d("TransactionFetchDS", "fetched ${dtos.size} transactions")
        dtos.mapNotNull { it.toEntity() }
    }.onFailure { Log.e("TransactionFetchDS", "fetch failed", it) }
}
