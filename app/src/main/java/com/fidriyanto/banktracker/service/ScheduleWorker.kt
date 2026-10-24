package com.fidriyanto.banktracker.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.fidriyanto.banktracker.domain.usecase.PostDueSchedulesUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

// ac: scheduled-transactions - adds the transactions of auto-post schedules that are due. It runs every
// few hours and each time the app starts. A date the phone missed is added the next time this runs.
@HiltWorker
class ScheduleWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val postDue: PostDueSchedulesUseCase,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = runCatching { postDue() }.fold(onSuccess = { Result.success() }, onFailure = { Result.retry() })

    companion object {
        private const val PERIODIC = "schedules_periodic"
        private const val NOW = "schedules_now"
        private const val PERIOD_HOURS = 6L

        fun schedule(context: Context) {
            val work = WorkManager.getInstance(context)
            work.enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.KEEP, PeriodicWorkRequestBuilder<ScheduleWorker>(PERIOD_HOURS, TimeUnit.HOURS).build())
            work.enqueueUniqueWork(NOW, ExistingWorkPolicy.REPLACE, OneTimeWorkRequestBuilder<ScheduleWorker>().build())
        }
    }
}
