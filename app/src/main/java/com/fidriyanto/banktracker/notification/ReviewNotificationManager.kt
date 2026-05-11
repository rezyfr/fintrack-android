package com.fidriyanto.banktracker.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import com.fidriyanto.banktracker.MainActivity
import com.fidriyanto.banktracker.R
import com.fidriyanto.banktracker.data.db.TransactionDao
import com.fidriyanto.banktracker.service.SyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReviewNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val transactionDao: TransactionDao
) {
    companion object {
        const val CHANNEL_ID = "transaction_review"
        const val EXTRA_TRANSACTION_ID = "transaction_id"
        const val REVIEW_TIMEOUT_MS = 3 * 60 * 1000L
    }

    init { createChannel() }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Transaction Review",
            NotificationManager.IMPORTANCE_HIGH
        ).apply { description = "Review auto-captured transactions" }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    suspend fun showReviewNotification(transactionId: Long) {
        val entity = transactionDao.getById(transactionId) ?: return

        val editIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_TRANSACTION_ID, transactionId)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val editPi = PendingIntent.getActivity(
            context, transactionId.toInt(), editIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val amountStr = if (entity.amount % 1.0 == 0.0) entity.amount.toInt().toString() else entity.amount.toString()
        val title = if (entity.category == "Transfer Out" && entity.channel == "PromptPay")
            "⚠ ฿$amountStr · ${entity.category}" else "฿$amountStr · ${entity.category}"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText("${entity.item} · Tap to edit")
            .setSubText("Auto-syncing in 3 min")
            .setContentIntent(editPi)
            .addAction(0, "Edit", editPi)
            .setAutoCancel(false)
            .setOngoing(true)
            .setTimeoutAfter(REVIEW_TIMEOUT_MS)
            .build()

        NotificationManagerCompat.from(context).notify(transactionId.toInt(), notification)

        val autoSync = OneTimeWorkRequestBuilder<SyncWorker>()
            .setInitialDelay(3, TimeUnit.MINUTES)
            .setInputData(workDataOf("transaction_id" to transactionId))
            .addTag("auto_sync_$transactionId")
            .build()
        WorkManager.getInstance(context).enqueue(autoSync)
    }

    fun dismiss(transactionId: Long) {
        NotificationManagerCompat.from(context).cancel(transactionId.toInt())
        WorkManager.getInstance(context).cancelAllWorkByTag("auto_sync_$transactionId")
    }
}
