package com.fidriyanto.banktracker.data.repository

import com.fidriyanto.banktracker.domain.model.Schedule
import kotlinx.coroutines.flow.Flow

interface ScheduleRepository {
    fun observe(): Flow<List<Schedule>>
    suspend fun all(): List<Schedule>
    suspend fun upsert(schedule: Schedule)
    suspend fun delete(id: String)
    suspend fun markPosted(id: String, through: String)
}
