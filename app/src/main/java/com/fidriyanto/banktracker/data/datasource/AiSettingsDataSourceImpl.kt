package com.fidriyanto.banktracker.data.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.fidriyanto.banktracker.domain.model.AiSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val NOTIFICATIONS_KEY = booleanPreferencesKey("ai_notifications")
private val SUGGESTIONS_KEY = booleanPreferencesKey("ai_suggestions")

@Singleton
class AiSettingsDataSourceImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : AiSettingsDataSource {
    private val defaults = AiSettings()

    override fun observe(): Flow<AiSettings> = dataStore.data.map { prefs ->
        AiSettings(
            notificationsEnabled = prefs[NOTIFICATIONS_KEY] ?: defaults.notificationsEnabled,
            suggestionsEnabled = prefs[SUGGESTIONS_KEY] ?: defaults.suggestionsEnabled,
        )
    }

    override suspend fun save(settings: AiSettings) {
        dataStore.edit { prefs ->
            prefs[NOTIFICATIONS_KEY] = settings.notificationsEnabled
            prefs[SUGGESTIONS_KEY] = settings.suggestionsEnabled
        }
    }
}
