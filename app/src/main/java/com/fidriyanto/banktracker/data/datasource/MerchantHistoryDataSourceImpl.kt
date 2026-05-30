package com.fidriyanto.banktracker.data.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val MERCHANT_HISTORY_KEY = stringPreferencesKey("merchant_history")
private const val MAX_HISTORY = 10

@Singleton
class MerchantHistoryDataSourceImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val gson: Gson,
) : MerchantHistoryDataSource {

    override fun observe(): Flow<List<String>> = dataStore.data.map { prefs ->
        val json = prefs[MERCHANT_HISTORY_KEY] ?: return@map emptyList()
        gson.fromJson(json, object : TypeToken<List<String>>() {}.type) ?: emptyList()
    }

    override suspend fun save(merchant: String) {
        val trimmed = merchant.trim()
        if (trimmed.isEmpty()) return
        dataStore.edit { prefs ->
            val current: List<String> = prefs[MERCHANT_HISTORY_KEY]?.let { json ->
                gson.fromJson(json, object : TypeToken<List<String>>() {}.type)
            } ?: emptyList()
            val deduped = listOf(trimmed) + current.filter {
                !it.equals(trimmed, ignoreCase = true)
            }
            prefs[MERCHANT_HISTORY_KEY] = gson.toJson(deduped.take(MAX_HISTORY))
        }
    }
}
