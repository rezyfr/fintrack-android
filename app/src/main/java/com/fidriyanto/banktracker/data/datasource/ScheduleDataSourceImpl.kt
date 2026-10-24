package com.fidriyanto.banktracker.data.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.fidriyanto.banktracker.domain.model.Schedule
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val KEY = stringPreferencesKey("schedules_json")

// The schedules are a short list that is read and written as a whole, so one JSON text is enough.
// Each change reads and writes inside one edit, so two writers cannot overwrite each other.
@Singleton
class ScheduleDataSourceImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : ScheduleDataSource {
    private val gson = Gson()
    private val type = object : TypeToken<List<Schedule>>() {}.type

    private fun read(prefs: Preferences): List<Schedule> =
        prefs[KEY]?.let { runCatching { gson.fromJson<List<Schedule>>(it, type) }.getOrNull() }.orEmpty()

    private suspend fun change(transform: (List<Schedule>) -> List<Schedule>) {
        dataStore.edit { it[KEY] = gson.toJson(transform(read(it))) }
    }

    override fun observe(): Flow<List<Schedule>> = dataStore.data.map(::read)
    override suspend fun all(): List<Schedule> = dataStore.data.first().let(::read)

    override suspend fun upsert(schedule: Schedule) = change { list ->
        if (list.any { it.id == schedule.id }) list.map { if (it.id == schedule.id) schedule else it } else list + schedule
    }

    override suspend fun delete(id: String) = change { list -> list.filterNot { it.id == id } }

    override suspend fun markPosted(id: String, through: String) = change { list ->
        list.map { if (it.id == id) it.copy(postedThrough = through) else it }
    }
}
