package com.fidriyanto.banktracker.data.datasource

import com.fidriyanto.banktracker.data.datasource.remote.SupabaseOverviewService
import com.fidriyanto.banktracker.data.datasource.remote.dto.ReconciliationRequest
import com.fidriyanto.banktracker.data.datasource.remote.dto.ReconciliationRowDto
import com.fidriyanto.banktracker.data.datasource.remote.dto.StatementBalanceUpsertDto
import com.fidriyanto.banktracker.data.datasource.remote.dto.WalletBalanceDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WalletBalanceDataSourceImpl @Inject constructor(
    private val service: SupabaseOverviewService,
) : WalletBalanceDataSource {

    override suspend fun getBalances(): List<WalletBalanceDto> = service.getWalletBalances()

    override suspend fun getReconciliation(month: String): List<ReconciliationRowDto> =
        service.getWalletReconciliation(ReconciliationRequest(month))

    override suspend fun upsertStatementBalance(walletId: String, month: String, openingBalance: Double) {
        val response = service.upsertStatementBalance(StatementBalanceUpsertDto(walletId, month, openingBalance))
        if (!response.isSuccessful) error("Supabase error: HTTP ${response.code()}")
    }
}
