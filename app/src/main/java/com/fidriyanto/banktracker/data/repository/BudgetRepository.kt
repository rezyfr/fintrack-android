package com.fidriyanto.banktracker.data.repository

import android.util.Log
import com.fidriyanto.banktracker.BuildConfig
import com.fidriyanto.banktracker.data.db.MonthlyBudgetEntity
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
class BudgetRepository @Inject constructor(
    private val httpClient: OkHttpClient
) {
    private val baseUrl get() = "${BuildConfig.SUPABASE_URL}/rest/v1/budgets"
    private val apiKey get() = BuildConfig.SUPABASE_ANON_KEY

    suspend fun get(): Result<List<MonthlyBudgetEntity>> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$baseUrl?select=*")
            .addHeader("apikey", apiKey)
            .build()
        return@withContext try {
            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: "[]"
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Supabase error: HTTP ${response.code}"))
            }
            val arr = JSONArray(body)
            val budgets = (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                MonthlyBudgetEntity(
                    currency = obj.getString("currency"),
                    bills = obj.getDouble("bills"),
                    subscriptions = obj.getDouble("subscriptions"),
                    entertainment = obj.getDouble("entertainment"),
                    foodDrink = obj.getDouble("food_drink"),
                    groceries = obj.getDouble("groceries"),
                    healthWellbeing = obj.getDouble("health_wellbeing"),
                    other = obj.getDouble("other"),
                    shopping = obj.getDouble("shopping"),
                    transport = obj.getDouble("transport"),
                    travel = obj.getDouble("travel"),
                    business = obj.getDouble("business"),
                    gifts = obj.getDouble("gifts")
                )
            }
            Result.success(budgets)
        } catch (e: Exception) {
            Log.e("BudgetRepository", "get failed", e)
            Result.failure(e)
        }
    }

    suspend fun upsert(budget: MonthlyBudgetEntity): Result<Unit> = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("currency", budget.currency)
            put("bills", budget.bills)
            put("subscriptions", budget.subscriptions)
            put("entertainment", budget.entertainment)
            put("food_drink", budget.foodDrink)
            put("groceries", budget.groceries)
            put("health_wellbeing", budget.healthWellbeing)
            put("other", budget.other)
            put("shopping", budget.shopping)
            put("transport", budget.transport)
            put("travel", budget.travel)
            put("business", budget.business)
            put("gifts", budget.gifts)
        }.toString()

        val request = Request.Builder()
            .url("$baseUrl?on_conflict=currency")
            .addHeader("apikey", apiKey)
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        return@withContext try {
            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string()
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Supabase error: HTTP ${response.code} — $responseBody"))
        } catch (e: Exception) {
            Log.e("BudgetRepository", "upsert failed", e)
            Result.failure(e)
        }
    }
}
