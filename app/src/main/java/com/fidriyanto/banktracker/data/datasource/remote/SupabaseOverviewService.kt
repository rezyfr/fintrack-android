package com.fidriyanto.banktracker.data.datasource.remote

import com.fidriyanto.banktracker.data.datasource.remote.dto.MonthlyOverviewRequest
import com.fidriyanto.banktracker.data.datasource.remote.dto.MonthlyOverviewRowDto
import com.fidriyanto.banktracker.data.datasource.remote.dto.TransportMerchantRowDto
import com.fidriyanto.banktracker.data.datasource.remote.dto.TransportMerchantsRequest
import com.fidriyanto.banktracker.data.datasource.remote.dto.ReconciliationRequest
import com.fidriyanto.banktracker.data.datasource.remote.dto.ReconciliationRowDto
import com.fidriyanto.banktracker.data.datasource.remote.dto.StatementBalanceUpsertDto
import com.fidriyanto.banktracker.data.datasource.remote.dto.WalletBalanceDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

interface SupabaseOverviewService {
    @POST("rest/v1/rpc/get_monthly_overview")
    suspend fun getMonthlyOverview(@Body body: MonthlyOverviewRequest): List<MonthlyOverviewRowDto>

    @POST("rest/v1/rpc/get_transport_merchants")
    suspend fun getTransportMerchants(@Body body: TransportMerchantsRequest): List<TransportMerchantRowDto>

    @GET("rest/v1/wallet_balances")
    suspend fun getWalletBalances(): List<WalletBalanceDto>

    @POST("rest/v1/rpc/get_wallet_reconciliation")
    suspend fun getWalletReconciliation(@Body body: ReconciliationRequest): List<ReconciliationRowDto>

    @POST("rest/v1/statement_balances")
    @Headers("Prefer: resolution=merge-duplicates,return=minimal")
    suspend fun upsertStatementBalance(@Body body: StatementBalanceUpsertDto): Response<ResponseBody>
}
