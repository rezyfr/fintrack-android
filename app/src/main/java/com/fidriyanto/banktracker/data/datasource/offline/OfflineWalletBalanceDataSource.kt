package com.fidriyanto.banktracker.data.datasource.offline

import com.fidriyanto.banktracker.data.datasource.WalletBalanceDataSource
import com.fidriyanto.banktracker.data.datasource.remote.dto.WalletBalanceAtDto
import com.fidriyanto.banktracker.data.datasource.remote.dto.WalletBalanceDto
import com.fidriyanto.banktracker.data.db.AccountDao
import com.fidriyanto.banktracker.data.db.LedgerDao
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

// A day after every transaction, so "up to this day" counts all of them.
private const val LAST_DAY = "9999-12-31"

// ac: offline-mode - account balances from the offline store, computed like the server's wallet_balances
// view and wallet_balances_at function.
@Singleton
class OfflineWalletBalanceDataSource @Inject constructor(
    private val accounts: AccountDao,
    private val dao: LedgerDao,
) : WalletBalanceDataSource {
    override suspend fun getBalances(): List<WalletBalanceDto> {
        val rows = accounts.observeAll().first()
        val balances = ledgerBalances(rows.associate { it.id to it.openingBalance }, dao.ownFlow(LAST_DAY), dao.incomingTransfers(LAST_DAY))
        return rows.sortedWith(compareBy({ it.sortOrder }, { it.id }))
            .map { WalletBalanceDto(it.id, it.name, it.currency, it.type, balances[it.id] ?: it.openingBalance) }
    }

    override suspend fun getBalancesAt(dates: List<String>): List<WalletBalanceAtDto> {
        val opening = accounts.observeAll().first().associate { it.id to it.openingBalance }
        return dates.distinct().sorted().flatMap { day ->
            val balances = ledgerBalances(opening, dao.ownFlow(day), dao.incomingTransfers(day))
            balances.keys.sorted().map { id -> WalletBalanceAtDto(id, day, balances.getValue(id)) }
        }
    }
}
