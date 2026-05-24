package com.fidriyanto.banktracker.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.fidriyanto.banktracker.data.repository.TransactionRepository
import com.fidriyanto.banktracker.notification.NotificationParser
import com.fidriyanto.banktracker.notification.ReviewNotificationManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BankNotificationService : NotificationListenerService() {
    companion object {
        private const val TAG = "BankNLS"
    }

    @Inject lateinit var repository: TransactionRepository
    @Inject lateinit var notificationManager: ReviewNotificationManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val title = sbn.notification.extras
            .getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString()
        val text = sbn.notification.extras
            .getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString()
        Log.d(TAG, "pkg=${sbn.packageName} title=$title text=$text")

        val parsed = NotificationParser.parse(title, text, sbn.postTime)
        Log.d(TAG, "parsed=$parsed")
        parsed ?: return

        scope.launch {
            val id = repository.processNewNotification(parsed) ?: return@launch
            notificationManager.showReviewNotification(id)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
