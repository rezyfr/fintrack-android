package com.fidriyanto.banktracker.sheets

import android.net.Uri
import android.util.Log
import com.fidriyanto.banktracker.auth.GoogleAuthManager
import com.fidriyanto.banktracker.data.db.MonthlyBudgetEntity
import com.fidriyanto.banktracker.data.db.MonthlyOverviewEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

class MonthlyOverviewFetcherImpl @Inject constructor(
    private val authManager: GoogleAuthManager,
    private val httpClient: OkHttpClient
) : MonthlyOverviewFetcher {

    companion object {
        private const val SPREADSHEET_ID = "SPREADSHEET_ID_REDACTED"
        private const val SHEETS_API = "https://sheets.googleapis.com/v4/spreadsheets"
    }

    override suspend fun fetch(): Result<MonthlyOverviewFetcher.FetchResult> {
        val token = authManager.getValidAccessToken()
            ?: return Result.failure(Exception("Not authenticated"))
        return try {
            val result = withContext(Dispatchers.IO) {
                coroutineScope {
                    val thbDeferred = async { fetchTab("Monthly Overview", "THB", token) }
                    val idrDeferred = async { fetchTab("Monthly Overview IDR", "IDR", token) }
                    val (thbRows, thbBudget) = thbDeferred.await()
                    val (idrRows, idrBudget) = idrDeferred.await()
                    MonthlyOverviewFetcher.FetchResult(
                        rows = thbRows + idrRows,
                        budgets = listOfNotNull(thbBudget, idrBudget)
                    )
                }
            }
            Result.success(result)
        } catch (e: Exception) {
            Log.e("MonthlyOverviewFetcher", "fetch failed", e)
            Result.failure(e)
        }
    }

    private fun fetchTab(
        tabName: String,
        currency: String,
        token: String
    ): Pair<List<MonthlyOverviewEntity>, MonthlyBudgetEntity?> {
        val range = Uri.encode("$tabName!A1:Q17")
        val url = "$SHEETS_API/$SPREADSHEET_ID/values/$range?valueRenderOption=UNFORMATTED_VALUE"
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .build()
        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("Sheets API error: HTTP ${response.code} for $tabName")
        }
        val body = response.body?.string() ?: throw Exception("Empty response for $tabName")
        return parseTab(body, currency)
    }

    private fun parseTab(
        json: String,
        currency: String
    ): Pair<List<MonthlyOverviewEntity>, MonthlyBudgetEntity?> {
        val values = JSONObject(json).getJSONArray("values")
        val rows = mutableListOf<MonthlyOverviewEntity>()
        var budget: MonthlyBudgetEntity? = null

        for (i in 1..12) {
            if (i >= values.length()) break
            val row = values.getJSONArray(i)
            val month = row.optString(0).trim()
            if (month.isBlank()) continue
            rows += MonthlyOverviewEntity(
                month = month,
                currency = currency,
                bills = row.cellDouble(1),
                subscriptions = row.cellDouble(2),
                entertainment = row.cellDouble(3),
                foodDrink = row.cellDouble(4),
                groceries = row.cellDouble(5),
                healthWellbeing = row.cellDouble(6),
                other = row.cellDouble(7),
                shopping = row.cellDouble(8),
                transport = row.cellDouble(9),
                travel = row.cellDouble(10),
                business = row.cellDouble(11),
                gifts = row.cellDouble(12),
                totalExpenditure = row.cellDouble(13),
                income = row.cellDouble(14),
                grossSavings = row.cellDouble(15)
            )
        }

        if (values.length() > 16) {
            val budgetRow = values.getJSONArray(16)
            budget = MonthlyBudgetEntity(
                currency = currency,
                bills = budgetRow.cellDouble(1),
                subscriptions = budgetRow.cellDouble(2),
                entertainment = budgetRow.cellDouble(3),
                foodDrink = budgetRow.cellDouble(4),
                groceries = budgetRow.cellDouble(5),
                healthWellbeing = budgetRow.cellDouble(6),
                other = budgetRow.cellDouble(7),
                shopping = budgetRow.cellDouble(8),
                transport = budgetRow.cellDouble(9),
                travel = budgetRow.cellDouble(10),
                business = budgetRow.cellDouble(11),
                gifts = budgetRow.cellDouble(12)
            )
        }

        return Pair(rows, budget)
    }

    private fun JSONArray.cellDouble(index: Int): Double =
        optDouble(index, 0.0).let { if (it.isNaN()) 0.0 else it }
}
