package com.fidriyanto.banktracker.data.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.fidriyanto.banktracker.domain.model.CustomBankPattern
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val KEY = stringPreferencesKey("custom_banks_json")

// ac: custom-bank-patterns - the patterns are a short list that is read and written as a whole, so one JSON
// text is enough. Each change reads and writes inside one edit, so two writers cannot overwrite each other.
@Singleton
class CustomBankDataSourceImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : CustomBankDataSource {
    private val gson = Gson()
    private val type = object : TypeToken<List<CustomBankPattern>>() {}.type

    private fun read(prefs: Preferences): List<CustomBankPattern> =
        prefs[KEY]?.let { runCatching { gson.fromJson<List<CustomBankPattern>>(it, type) }.getOrNull() }.orEmpty()

    private suspend fun change(transform: (List<CustomBankPattern>) -> List<CustomBankPattern>) {
        dataStore.edit { it[KEY] = gson.toJson(transform(read(it))) }
    }

    override fun observe(): Flow<List<CustomBankPattern>> = dataStore.data.map(::read)
    override suspend fun all(): List<CustomBankPattern> = dataStore.data.first().let(::read)

    override suspend fun upsert(pattern: CustomBankPattern) = change { list ->
        if (list.any { it.id == pattern.id }) list.map { if (it.id == pattern.id) pattern else it } else list + pattern
    }

    override suspend fun delete(id: String) = change { list -> list.filterNot { it.id == id } }
}
