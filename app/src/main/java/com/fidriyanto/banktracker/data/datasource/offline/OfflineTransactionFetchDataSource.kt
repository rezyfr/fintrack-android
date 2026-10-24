package com.fidriyanto.banktracker.data.datasource.offline

import androidx.sqlite.db.SimpleSQLiteQuery
import com.fidriyanto.banktracker.data.datasource.TransactionFetchDataSource
import com.fidriyanto.banktracker.data.datasource.remote.mapper.toEntity
import com.fidriyanto.banktracker.data.db.LedgerDao
import com.fidriyanto.banktracker.data.db.TransactionEntity
import javax.inject.Inject
import javax.inject.Singleton

// The same limits as the server fetch: a range closed at both ends reads up to 25 pages of 200, and an
// open-ended fetch keeps the first page only.
private const val PAGE_SIZE = 200
private const val MAX_ROWS = PAGE_SIZE * 25

// ac: offline-mode - the transaction list from the offline store.
@Singleton
class OfflineTransactionFetchDataSource @Inject constructor(
    private val dao: LedgerDao,
) : TransactionFetchDataSource {
    override suspend fun fetch(
        month: String?, wallet: String?, txType: String?, category: String?,
        search: String?, amountMin: Double?, amountMax: Double?,
        dateFrom: String?, dateTo: String?, involvingWallet: String?,
    ): Result<List<TransactionEntity>> = runCatching {
        val hasRange = dateFrom != null || dateTo != null
        val bounded = (dateFrom != null && dateTo != null) || (!hasRange && month != null)
        val query = ledgerTransactionQuery(
            month, wallet, txType, category, search, amountMin, amountMax, dateFrom, dateTo, involvingWallet,
            limit = if (bounded) MAX_ROWS else PAGE_SIZE,
        )
        dao.queryTransactions(SimpleSQLiteQuery(query.sql, query.args.toTypedArray())).mapNotNull { it.toDto().toEntity() }
    }

    override suspend fun recentTransferRates(fromWallets: List<String>, toWallets: List<String>, limit: Int): Result<List<Double>> = runCatching {
        if (fromWallets.isEmpty() || toWallets.isEmpty()) return@runCatching emptyList()
        dao.transfers(fromWallets, toWallets, limit).mapNotNull { row -> row.toAmount?.let { it / row.amount } }
    }
}
