package com.fidriyanto.banktracker.service

import android.content.ComponentName
import android.content.Context
import android.service.notification.NotificationListenerService
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

// ac: notification-listener-rebinds — Android unbinds the notification listener when it stops the
// app, and does not always bind it again. The permission still reads as granted in Settings, so the
// app looks healthy while it receives nothing and every bank notification is missed in silence.
//
// BankNotificationService asks for a rebind when it is told it was disconnected, but a process the
// system kills outright is never told. This worker covers that case: WorkManager starts the process
// to run it, and the check then runs in a process that knows whether the listener is live.
@HiltWorker
class ListenerHealthWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        // Nothing to repair when the user never granted notification access. Asking to rebind a
        // listener the user turned off would be noise, not repair.
        val granted = NotificationManagerCompat.getEnabledListenerPackages(context)
            .contains(context.packageName)
        // The worker runs in the same process as the service, so isConnected is an exact answer to
        // "is the listener bound right now", not a guess from how long ago something last happened.
        // A process the system killed starts again with the flag false, which is the case to repair.
        if (!needsRebind(granted, BankNotificationService.isConnected)) return Result.success()

        Log.w(TAG, "listener is not connected, requesting rebind")
        NotificationListenerService.requestRebind(
            ComponentName(context, BankNotificationService::class.java),
        )
        return Result.success()
    }

    companion object {
        private const val TAG = "BankNLSHealth"
        private const val UNIQUE_NAME = "listener_health"

        // The whole decision, kept apart from the Android calls so it can be read and tested on its
        // own. Repair only what is broken: a listener the user never granted is not broken, and one
        // that is bound does not want a rebind, because a needless rebind is itself a gap in cover.
        internal fun needsRebind(granted: Boolean, connected: Boolean): Boolean = granted && !connected

        // Half an hour is a compromise. Shorter misses less, but the system defers work in Doze
        // anyway, so a tighter period buys less than it looks like it would.
        private const val PERIOD_MINUTES = 30L

        // KEEP, so an app already running keeps its schedule instead of restarting the period on
        // every launch. WorkManager holds the schedule across process death and across a reboot.
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ListenerHealthWorker>(
                PERIOD_MINUTES,
                TimeUnit.MINUTES,
            ).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(UNIQUE_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
