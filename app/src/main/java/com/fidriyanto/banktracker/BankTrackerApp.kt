package com.fidriyanto.banktracker

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import com.fidriyanto.banktracker.service.ListenerHealthWorker
import com.fidriyanto.banktracker.service.ListenerRepair
import com.fidriyanto.banktracker.service.ScheduleWorker

@HiltAndroidApp
class BankTrackerApp : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory
    override val workManagerConfiguration get() =
        Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        // ac: notification-listener-rebinds — the schedule is kept by WorkManager across process
        // death and across a reboot, so this only has to enqueue it once. Scheduling here means a
        // process the system started for any reason puts the check back in place.
        ListenerHealthWorker.schedule(this)
        // ac: listener-repair - a process that starts and finds the listener stopped brings it back
        ListenerRepair.checkSoon(this)
        // ac: scheduled-transactions - add the transactions that are due, now and every few hours
        ScheduleWorker.schedule(this)
    }
}
