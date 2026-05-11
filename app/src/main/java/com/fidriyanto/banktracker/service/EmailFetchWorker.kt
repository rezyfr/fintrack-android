package com.fidriyanto.banktracker.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fidriyanto.banktracker.data.repository.TransactionRepository
import com.fidriyanto.banktracker.notification.ReviewNotificationManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class EmailFetchWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: TransactionRepository,
    private val notificationManager: ReviewNotificationManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val triggerAmount = inputData.getDouble("trigger_amount", 0.0)
        val transactionId = repository.processNewNotification(triggerAmount) ?: return Result.success()
        notificationManager.showReviewNotification(transactionId)
        return Result.success()
    }
}
