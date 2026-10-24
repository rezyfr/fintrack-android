package com.fidriyanto.banktracker.data.repository

import com.fidriyanto.banktracker.data.datasource.ScheduleDataSource
import com.fidriyanto.banktracker.domain.model.Schedule
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduleRepositoryImpl @Inject constructor(
    private val local: ScheduleDataSource,
) : ScheduleRepository {
    override fun observe(): Flow<List<Schedule>> = local.observe()
    override suspend fun all(): List<Schedule> = local.all()
    override suspend fun upsert(schedule: Schedule) = local.upsert(schedule)
    override suspend fun delete(id: String) = local.delete(id)
    override suspend fun markPosted(id: String, through: String) = local.markPosted(id, through)
}
