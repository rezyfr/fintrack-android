package com.fidriyanto.banktracker.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class BankNotificationService : NotificationListenerService() {
    companion object {
        const val BANGKOK_BANK_PACKAGE = "th.co.bangkokbank.bangkokmobile"
    }
    override fun onNotificationPosted(sbn: StatusBarNotification) {}
}
