package com.buysloans.hub

import android.app.Application

class MorleyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)
        UpdateCheckScheduler.schedule(this)
    }
}
