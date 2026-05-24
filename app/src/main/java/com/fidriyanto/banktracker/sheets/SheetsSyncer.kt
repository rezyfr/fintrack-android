package com.fidriyanto.banktracker.sheets

import android.net.Uri
import android.util.Log
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
interface SheetsSyncer {
    suspend fun sync(row: SheetsRow): Result<Unit>
}
