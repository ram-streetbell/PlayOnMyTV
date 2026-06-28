package com.playonmytv.sync

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.playonmytv.worker.SyncWorker

class DeviceSyncService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SYNC_NOW -> SyncWorker.enqueueImmediate(applicationContext)
            ACTION_SCHEDULE_PERIODIC -> SyncWorker.enqueuePeriodic(applicationContext)
            else -> SyncWorker.enqueuePeriodic(applicationContext)
        }

        stopSelf(startId)
        return START_NOT_STICKY
    }

    companion object {
        private const val ACTION_SYNC_NOW = "com.playonmytv.action.SYNC_NOW"
        private const val ACTION_SCHEDULE_PERIODIC = "com.playonmytv.action.SCHEDULE_PERIODIC_SYNC"

        fun requestImmediateSync(context: Context) {
            context.startService(
                Intent(context, DeviceSyncService::class.java).setAction(ACTION_SYNC_NOW)
            )
        }

        fun schedulePeriodicSync(context: Context) {
            SyncWorker.enqueuePeriodic(context.applicationContext)
        }
    }
}
