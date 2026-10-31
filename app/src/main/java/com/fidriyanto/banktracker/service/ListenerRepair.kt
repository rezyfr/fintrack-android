package com.fidriyanto.banktracker.service

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ac: listener-repair - on some phones Android stops the notification listener and then does not bind it again, even
// though the access is still on. requestRebind alone did nothing on the phone this was found on: the app's service
// record showed no run for days. Switching the listener component off and on makes Android bind it again.
object ListenerRepair {
    private const val TAG = "BankNLSRepair"
    private const val CHECK_DELAY_MS = 5_000L
    private const val MIN_GAP_MS = 2 * 60_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    @Volatile private var lastRepair = 0L

    fun hasAccess(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    // Asks for a rebind, then switches the listener off and on. The app is not restarted.
    fun reconnect(context: Context) {
        val component = ComponentName(context, BankNotificationService::class.java)
        Log.w(TAG, "reconnecting the listener")
        lastRepair = System.currentTimeMillis()
        runCatching { NotificationListenerService.requestRebind(component) }
        val pm = context.packageManager
        pm.setComponentEnabledSetting(component, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
        pm.setComponentEnabledSetting(component, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
    }

    // Called when the app process starts. Android needs a moment to bind the listener on its own, so the check waits.
    // It repairs a listener that is still not connected, at most once every two minutes.
    fun checkSoon(context: Context) {
        val app = context.applicationContext
        scope.launch {
            delay(CHECK_DELAY_MS)
            val gap = System.currentTimeMillis() - lastRepair
            if (needsRepair(hasAccess(app), BankNotificationService.isConnected) && gap > MIN_GAP_MS) reconnect(app)
        }
    }

    // The access is on, but the listener is not running
    internal fun needsRepair(granted: Boolean, connected: Boolean): Boolean = granted && !connected
}
