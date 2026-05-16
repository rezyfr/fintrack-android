package com.fidriyanto.banktracker.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.work.*
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class BankNotificationService : NotificationListenerService() {
    companion object {
        const val BANGKOK_BANK_PACKAGE = "th.co.bangkokbank.bangkokmobile"
        private const val TAG = "BankNLS"
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        Log.d(TAG, "onNotificationPosted: pkg=${sbn.packageName}")
//        if (sbn.packageName != BANGKOK_BANK_PACKAGE) return
        val text = sbn.notification.extras
            .getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString()
        Log.d(TAG, "EXTRA_TEXT=$text")
        text ?: return
        val amount = extractAmount(text)
        Log.d(TAG, "amount=$amount")
        amount ?: return

        val work = OneTimeWorkRequestBuilder<EmailFetchWorker>()
            .setInitialDelay(5, TimeUnit.SECONDS)
            .setInputData(workDataOf("trigger_amount" to amount))
            .setConstraints(Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(applicationContext).enqueue(work)
    }

    private fun extractAmount(text: String): Double? {
        val regex = Regex("""(\d[\d,]*(?:\.\d{1,2})?)THB""", RegexOption.IGNORE_CASE)
        return regex.find(text)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()
    }
}
