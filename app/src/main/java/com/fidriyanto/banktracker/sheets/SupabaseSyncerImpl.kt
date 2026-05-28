package com.fidriyanto.banktracker.sheets

import android.util.Log
import com.fidriyanto.banktracker.BuildConfig
import com.fidriyanto.banktracker.data.model.SheetsRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject

class SupabaseSyncerImpl @Inject constructor(
    private val httpClient: OkHttpClient
) : SheetsSyncer {

    override suspend fun sync(row: SheetsRow): Result<Unit> = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("tab",      row.tab.name)
            put("date",     row.date.toString())
            put("merchant", row.merchant)
            put("item",     row.item)
            put("amount",   row.amount)
            put("category", row.category)
            put("channel",  row.channel)
            if (row.note     != null) put("note",      row.note)
            if (row.wallet   != null) put("wallet",    row.wallet)
            put("tx_type", row.txType)
            if (row.toWallet != null) put("to_wallet", row.toWallet)
        }.toString()

        val request = Request.Builder()
            .url("${BuildConfig.SUPABASE_URL}/rest/v1/transactions")
            .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=minimal")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        Log.d("SupabaseSyncer", "POST transactions: $body")
        return@withContext try {
            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string()
            Log.d("SupabaseSyncer", "status=${response.code} body=$responseBody")
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Supabase error: HTTP ${response.code} — $responseBody"))
        } catch (e: Exception) {
            Log.e("SupabaseSyncer", "sync failed", e)
            Result.failure(e)
        }
    }
}
