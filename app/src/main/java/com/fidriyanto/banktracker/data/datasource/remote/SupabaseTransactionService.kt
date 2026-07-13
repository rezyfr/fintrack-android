package com.fidriyanto.banktracker.data.datasource.remote

import com.fidriyanto.banktracker.data.datasource.remote.dto.CategoryPatchDto
import com.fidriyanto.banktracker.data.datasource.remote.dto.TransactionDto
import com.fidriyanto.banktracker.data.datasource.remote.dto.TransactionInsertDto
import com.fidriyanto.banktracker.data.datasource.remote.dto.TransactionPatchDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query

interface SupabaseTransactionService {
    @GET("rest/v1/transactions")
    suspend fun fetchTransactions(
        @Query("order") order: String,
        @Query("limit") limit: Int,
        @Query("date") dateFilters: List<String>?,
        @Query("wallet") wallet: String?,
        @Query("tx_type") txType: String?,
        @Query("category") category: String?,
        // ac: advanced-transaction-filters
        @Query("item") itemFilter: String? = null,
        @Query("amount") amountFilters: List<String>? = null,
    ): List<TransactionDto>

    @POST("rest/v1/transactions")
    @Headers("Prefer: return=minimal")
    suspend fun insertTransaction(@Body body: TransactionInsertDto): Response<ResponseBody>

    @DELETE("rest/v1/transactions")
    suspend fun deleteTransaction(@Query("id") idFilter: String): Response<ResponseBody>

    @PATCH("rest/v1/transactions")
    @Headers("Prefer: return=minimal")
    suspend fun updateTransaction(
        @Query("id") idFilter: String,
        @Body body: TransactionPatchDto,
    ): Response<ResponseBody>

    // ac: batch-edit-transaction-category — idFilter is an "in.(1,2,3)" postgrest filter matching multiple rows
    @PATCH("rest/v1/transactions")
    @Headers("Prefer: return=minimal")
    suspend fun updateCategoryBatch(
        @Query("id") idFilter: String,
        @Body body: CategoryPatchDto,
    ): Response<ResponseBody>
}
