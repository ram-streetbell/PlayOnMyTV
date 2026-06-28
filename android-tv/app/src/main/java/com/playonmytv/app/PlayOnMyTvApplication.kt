package com.playonmytv.app

import android.app.Application
import com.playonmytv.sync.DeviceSyncService

class PlayOnMyTvApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DeviceSyncService.schedulePeriodicSync(this)
    }
}
