package com.fidriyanto.banktracker.email

import android.net.Uri
import android.util.Log
import com.fidriyanto.banktracker.auth.GoogleAuthManager
import com.fidriyanto.banktracker.data.prefs.SecurePrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmailFetcher @Inject constructor(
    private val authManager: GoogleAuthManager,
    private val prefs: SecurePrefs,
    private val httpClient: OkHttpClient
) {
    companion object {
        private const val GMAIL_API = "https://gmail.googleapis.com/gmail/v1/users/me"
    }

    suspend fun fetchLatestBankEmail(): String? = withContext(Dispatchers.IO) {
        val token = authManager.getValidAccessToken() ?: run {
            Log.w("EmailFetcher", "no access token"); return@withContext null
        }
        val query = prefs.gmailSenderFilter
        Log.d("EmailFetcher", "querying: $query")

        val listUrl = "$GMAIL_API/messages?q=${Uri.encode(query)}&maxResults=1"
        val listReq = Request.Builder().url(listUrl)
            .addHeader("Authorization", "Bearer $token").build()
        val listBody = httpClient.newCall(listReq).execute().also {
            Log.d("EmailFetcher", "list status=${it.code}")
        }.body?.string() ?: return@withContext null
        val messageId = JSONObject(listBody)
            .optJSONArray("messages")?.optJSONObject(0)?.optString("id")
            ?: run { Log.w("EmailFetcher", "no messages found: $listBody"); return@withContext null }

        Log.d("EmailFetcher", "fetching messageId=$messageId")
        val msgUrl = "$GMAIL_API/messages/$messageId?format=full"
        val msgReq = Request.Builder().url(msgUrl)
            .addHeader("Authorization", "Bearer $token").build()
        val msgBody = JSONObject(httpClient.newCall(msgReq).execute().body?.string() ?: return@withContext null)

        extractHtmlBody(msgBody)
    }

    private fun extractHtmlBody(message: JSONObject): String? {
        val payload = message.optJSONObject("payload") ?: return null
        val directData = payload.optJSONObject("body")?.optString("data")
        if (!directData.isNullOrEmpty()) return decodeBase64Url(directData)
        val parts = payload.optJSONArray("parts") ?: return null
        for (i in 0 until parts.length()) {
            val part = parts.getJSONObject(i)
            if (part.optString("mimeType") == "text/html") {
                val data = part.optJSONObject("body")?.optString("data") ?: continue
                return decodeBase64Url(data)
            }
        }
        return null
    }

    private fun decodeBase64Url(encoded: String): String {
        val bytes = android.util.Base64.decode(encoded, android.util.Base64.URL_SAFE)
        return String(bytes, Charsets.UTF_8)
    }
}
