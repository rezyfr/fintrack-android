package com.fidriyanto.banktracker.data.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.fidriyanto.banktracker.data.model.RuleDocument
import com.fidriyanto.banktracker.domain.model.Rule
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val KEY = stringPreferencesKey("rules_json")
private val UPDATED_KEY = longPreferencesKey("rules_updated_ms")
private val DIRTY_KEY = booleanPreferencesKey("rules_dirty")

// The rules are a short list that is always read and written as a whole, so one JSON text is enough.
@Singleton
class RuleDataSourceImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : RuleDataSource {
    private val gson = Gson()
    private val type = object : TypeToken<List<Rule>>() {}.type

    private fun read(prefs: Preferences): List<Rule> =
        prefs[KEY]?.let { runCatching { gson.fromJson<List<Rule>>(it, type) }.getOrNull() }.orEmpty()

    override fun observe(): Flow<List<Rule>> = dataStore.data.map(::read)

    override suspend fun document(): RuleDocument = dataStore.data.first().let { RuleDocument(read(it), it[UPDATED_KEY] ?: 0L) }

    override suspend fun isDirty(): Boolean = dataStore.data.first()[DIRTY_KEY] ?: false

    override suspend fun save(document: RuleDocument, dirty: Boolean) {
        dataStore.edit {
            it[KEY] = gson.toJson(document.rules)
            it[UPDATED_KEY] = document.updatedAtMs
            it[DIRTY_KEY] = dirty
        }
    }

    override suspend fun markSynced() {
        dataStore.edit { it[DIRTY_KEY] = false }
    }
}
