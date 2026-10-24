package com.fidriyanto.banktracker.data.datasource.remote

import com.fidriyanto.banktracker.data.datasource.remote.dto.CategoryBatchPatchDto
import com.fidriyanto.banktracker.data.datasource.remote.dto.TransactionDto
import com.fidriyanto.banktracker.data.datasource.remote.dto.TransactionInsertDto
import com.fidriyanto.banktracker.data.datasource.remote.dto.TransactionPageDto
import com.fidriyanto.banktracker.data.datasource.remote.dto.TransactionPatchDto
import com.fidriyanto.banktracker.data.datasource.remote.dto.TransferRatesDto
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface TransactionApiService {
    @GET("v1/transactions")
    suspend fun fetchTransactions(
        @Query("month") month: String?,
        @Query("date_from") dateFrom: String?,
        @Query("date_to") dateTo: String?,
        @Query("wallet") wallet: String?,
        @Query("involving_wallet") involvingWallet: String?,
        @Query("tx_type") txType: String?,
        @Query("category") category: String?,
        @Query("search") search: String?,
        @Query("amount_min") amountMin: Double?,
        @Query("amount_max") amountMax: Double?,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int?,
    ): TransactionPageDto

    // ac: add-transfer-target-amount — the exchange rates of the user's own cross-currency transfers, newest first
    @GET("v1/transfer-rates")
    suspend fun fetchTransferRates(
        @Query("from_wallets") fromWallets: String,
        @Query("to_wallets") toWallets: String,
        @Query("limit") limit: Int,
    ): TransferRatesDto

    @POST("v1/transactions")
    suspend fun insertTransaction(@Body body: TransactionInsertDto): Response<TransactionDto>

    @DELETE("v1/transactions/{id}")
    suspend fun deleteTransaction(@Path("id") id: Long): Response<ResponseBody>

    @PATCH("v1/transactions/{id}")
    suspend fun updateTransaction(
        @Path("id") id: Long,
        @Body body: TransactionPatchDto,
    ): Response<ResponseBody>

    // ac: installment-link-edit — a plain PATCH body drops null fields, so unlinking sends a raw body
    @PATCH("v1/transactions/{id}")
    suspend fun clearInstallment(
        @Path("id") id: Long,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    // ac: batch-edit-transaction-category — one request changes the category of every listed id
    @PATCH("v1/transactions")
    suspend fun updateCategoryBatch(@Body body: CategoryBatchPatchDto): Response<ResponseBody>
}
