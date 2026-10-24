package com.fidriyanto.banktracker.domain.usecase

import com.fidriyanto.banktracker.data.repository.ScheduleRepository
import com.fidriyanto.banktracker.domain.model.Schedule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject

// ac: scheduled-transactions - read, save, delete and switch auto-post of the schedules
class ManageSchedulesUseCase @Inject constructor(
    private val schedules: ScheduleRepository,
) {
    fun observe(): Flow<List<Schedule>> = schedules.observe()
    suspend fun find(id: String): Schedule? = schedules.all().firstOrNull { it.id == id }
    suspend fun delete(id: String) = schedules.delete(id)

    // A new schedule with auto-post on starts from today, so its past dates are never posted.
    suspend fun save(schedule: Schedule, today: LocalDate = LocalDate.now()) {
        val old = find(schedule.id)
        val fresh = schedule.autoPost && old?.autoPost != true
        schedules.upsert(if (fresh) schedule.copy(postedThrough = today.minusDays(1).toString()) else schedule.copy(postedThrough = old?.postedThrough ?: schedule.postedThrough))
    }

    suspend fun setAutoPost(id: String, on: Boolean, today: LocalDate = LocalDate.now()) {
        val s = schedules.observe().first().firstOrNull { it.id == id } ?: return
        save(s.copy(autoPost = on), today)
    }
}
