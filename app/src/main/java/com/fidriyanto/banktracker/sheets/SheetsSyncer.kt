package com.fidriyanto.banktracker.sheets

import android.net.Uri
import android.util.Log
import com.fidriyanto.banktracker.auth.GoogleAuthManager
import com.fidriyanto.banktracker.data.model.SheetsRow
import com.fidriyanto.banktracker.data.model.SheetTab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SheetsSyncer @Inject constructor(
    private val authManager: GoogleAuthManager,
    private val httpClient: OkHttpClient
) {
    companion object {
        private const val SPREADSHEET_ID = "1OJqLIPFWjJPje8HabLVyMp_AGvsCi9nWM2nHqdzv9-w"
        private const val SHEETS_API = "https://sheets.googleapis.com/v4/spreadsheets"
    }

    suspend fun sync(row: SheetsRow): Result<Unit> = withContext(Dispatchers.IO) {
        val token = authManager.getValidAccessToken()
            ?: return@withContext Result.failure(Exception("Not authenticated"))

        val (tabName, values) = buildRowValues(row)
        val range = Uri.encode("$tabName!A:E")
        val url = "$SHEETS_API/$SPREADSHEET_ID/values/$range:append?valueInputOption=USER_ENTERED"

        val bodyJson = JSONObject().put("values", JSONArray().put(JSONArray(values))).toString()
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Content-Type", "application/json")
            .post(bodyJson.toRequestBody("application/json".toMediaType()))
            .build()

        Log.d("SheetsSyncer", "POST $url")
        Log.d("SheetsSyncer", "body=$bodyJson")
        return@withContext try {
            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string()
            Log.d("SheetsSyncer", "status=${response.code} body=$responseBody")
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Sheets API error: HTTP ${response.code} — $responseBody"))
        } catch (e: Exception) {
            Log.e("SheetsSyncer", "sync failed", e)
            Result.failure(e)
        }
    }

    private fun buildRowValues(row: SheetsRow): Pair<String, List<Any>> {
        val amountStr = row.amount.toAmountString()
        return when (row.tab) {
            SheetTab.EXPENSES -> Pair("Expenses", listOf(
                DateFormatter.toExpensesDate(row.date), row.item, amountStr, row.category
            ))
            SheetTab.IDR_EXPENSES -> Pair("IDR Expenses", listOf(
                DateFormatter.toExpensesDate(row.date), row.item, amountStr, row.category
            ))
            SheetTab.INCOME -> Pair("Income", listOf(
                "", DateFormatter.toIncomeDate(row.date), row.item, "", amountStr
            ))
            SheetTab.IDR_INCOME -> Pair("IDR Income", listOf(
                "", DateFormatter.toIncomeDate(row.date), row.item, "", amountStr
            ))
        }
    }

    private fun Double.toAmountString(): String =
        if (this % 1.0 == 0.0) this.toInt().toString() else this.toString()
}
