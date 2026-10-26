package com.fidriyanto.banktracker.service

import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.fidriyanto.banktracker.data.model.ParsedTransaction
import com.fidriyanto.banktracker.data.repository.TransactionRepository
import com.fidriyanto.banktracker.domain.usecase.ApplyRulesUseCase
import com.fidriyanto.banktracker.domain.model.AutoSource
import com.fidriyanto.banktracker.domain.usecase.ParseCustomBankUseCase
import com.fidriyanto.banktracker.domain.usecase.ParseNotificationWithAiUseCase
import com.fidriyanto.banktracker.domain.usecase.RecordAutoAddedUseCase
import com.fidriyanto.banktracker.domain.usecase.ResolveBankWalletUseCase
import com.fidriyanto.banktracker.notification.NotificationParser
import com.fidriyanto.banktracker.notification.notificationTime
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

        // ac: notification-listener-rebinds — ListenerHealthWorker runs in this same process, so an
        // in-memory flag answers "is the listener bound right now" exactly. A process the system
        // killed starts again with it false, which is the case the worker repairs.
        @Volatile
        var isConnected: Boolean = false
            private set
    }

    @Inject lateinit var repository: TransactionRepository
    @Inject lateinit var reviewNotificationManager: ReviewNotificationManager
    @Inject lateinit var parseWithAi: ParseNotificationWithAiUseCase
    @Inject lateinit var parseCustomBank: ParseCustomBankUseCase
    @Inject lateinit var resolveBankWallet: ResolveBankWalletUseCase
    @Inject lateinit var applyRules: ApplyRulesUseCase
    @Inject lateinit var recordAutoAdded: RecordAutoAddedUseCase

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification) = handle(sbn, catchUp = false)

    // ac: catch-up-on-connect - [catchUp] is true for a notification that was already in the shade when the listener
    // connected. Those go through the patterns only. The on-device model is left out, so a reconnect does not run
    // it on every payment-looking notification that is still waiting in the shade.
    private fun handle(sbn: StatusBarNotification, catchUp: Boolean) {
        val title = sbn.notification.extras
            .getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString()
        val text = sbn.notification.extras
            .getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString()
        if (!catchUp) Log.d(TAG, "pkg=${sbn.packageName} title=$title text=$text")

        // ac: repost-duplicate - one time for the whole notification, so a repost of it is not saved twice
        val at = notificationTime(sbn.notification.`when`, sbn.postTime)
        val parsed = NotificationParser.parse(title, text, at)
        if (!catchUp) Log.d(TAG, "parsed=$parsed")
        // This app's own review notifications are never read.
        if (title == null || text == null || sbn.packageName == packageName) return
        scope.launch {
            // ac: bank-parser-accounts - a bank pattern saves to the account the user chose for that bank
            val fromPattern = parsed?.let { resolveBankWallet(it) }
            if (fromPattern != null) return@launch capture(fromPattern, sbn.postTime)
            // ac: custom-bank-patterns - a bank the user taught the app is read before the on-device model
            parseCustomBank(title, text, at)?.let { return@launch capture(it, sbn.postTime) }
            // ac: on-device-ai - a payment the patterns do not know, or a bank the user turned off or has
            // no account for, goes to the on-device model.
            if (!catchUp && NotificationParser.looksLikePayment(title, text)) parseWithAi(title, text, at)?.let { capture(it, sbn.postTime) }
        }
    }

    private suspend fun capture(read: ParsedTransaction, postTime: Long) {
        // ac: custom-rules - the user's rules set the category and the description, or drop the transaction
        val parsed = applyRules.forCapture(read) ?: return
        val id = repository.processNewNotification(parsed, alsoSeenAt = postTime) ?: return
        reviewNotificationManager.showCapturedNotification(id)
        // ac: auto-added-notice - Home tells the user about it until they dismiss it. A failure here never undoes the capture.
        runCatching { recordAutoAdded(parsed.item, parsed.amount, parsed.txType, parsed.wallet, AutoSource.NOTIFICATION) }
    }

    // ac: notification-listener-rebinds — Android unbinds the listener when it stops the app, and does
    // not always bind it again. The permission stays granted in Settings while the service receives
    // nothing, so the app looks healthy and silently misses every notification. That is how a Bangkok
    // Bank withdrawal went uncaptured: the pattern reads that text, but nothing delivered it.
    //
    // Asking for a rebind is the only repair an app can make from its own side, and it only works
    // while the process is alive to be told. A process the system kills outright never sees this.
    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isConnected = false
        Log.w(TAG, "listener disconnected, requesting rebind")
        requestRebind(ComponentName(this, BankNotificationService::class.java))
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        isConnected = true
        Log.d(TAG, "listener connected")
        // ac: catch-up-on-connect - Android can stop this app for memory, and a payment that arrives while it is
        // stopped is never delivered. When the listener connects again, the notifications still in the shade are read.
        // One that was already saved is skipped by the duplicate check.
        runCatching { activeNotifications }.getOrNull()?.forEach { handle(it, catchUp = true) }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
