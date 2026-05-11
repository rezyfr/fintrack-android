package com.fidriyanto.banktracker.categorization

import com.fidriyanto.banktracker.data.db.CategoryCacheDao
import com.fidriyanto.banktracker.data.db.CategoryCacheEntity
import com.fidriyanto.banktracker.email.MerchantNormalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClaudeCategorizor @Inject constructor(
    private val cacheDao: CategoryCacheDao,
    private val httpClient: OkHttpClient
) {
    companion object {
        val CATEGORIES = listOf(
            "Bills", "Subscriptions", "Entertainment", "Food & Drink", "Groceries",
            "Health & Wellbeing", "Other", "Shopping", "Transport", "Travel",
            "Business", "Gifts", "Transfer Out"
        )
        private const val MODEL = "claude-haiku-4-5-20251001"
        private const val API_URL = "https://api.anthropic.com/v1/messages"
    }

    suspend fun categorize(
        merchant: String,
        amount: Double,
        channel: String,
        apiKey: String
    ): Pair<String, String> = withContext(Dispatchers.IO) {
        val cacheKey = MerchantNormalizer.normalize(merchant)
        cacheDao.getByMerchant(cacheKey)?.let {
            return@withContext Pair(it.category, it.cleanDescription)
        }

        val prompt = buildPrompt(merchant, amount, channel)
        try {
            val body = """{"model":"$MODEL","max_tokens":100,"messages":[{"role":"user","content":${JSONObject.quote(prompt)}}]}"""
            val request = Request.Builder()
                .url(API_URL)
                .addHeader("x-api-key", apiKey)
                .addHeader("anthropic-version", "2023-06-01")
                .addHeader("content-type", "application/json")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            val text = JSONObject(response.body!!.string())
                .getJSONArray("content").getJSONObject(0).getString("text").trim()
            val json = JSONObject(text)
            val category = json.getString("category").takeIf { it in CATEGORIES } ?: "Other"
            val description = json.optString("description", merchant)

            cacheDao.insert(CategoryCacheEntity(cacheKey, category, description))
            Pair(category, description)
        } catch (e: Exception) {
            Pair("Other", merchant)
        }
    }

    private fun buildPrompt(merchant: String, amount: Double, channel: String) = """
        Categorize this Bangkok Bank transaction. Return JSON only.
        Merchant: $merchant
        Amount: ${amount.toInt()} THB
        Channel: $channel
        Categories: ${CATEGORIES.joinToString(", ")}
        Response format: {"category":"...","description":"..."}
        description = clean 2-4 word item name (e.g. "TrueMoney top-up", "BTS fare")
    """.trimIndent()
}
