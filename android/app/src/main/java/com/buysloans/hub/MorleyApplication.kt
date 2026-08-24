package com.buysloans.hub

import android.app.Application
import com.google.firebase.messaging.FirebaseMessaging

class MorleyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)
        UpdateCheckScheduler.schedule(this)
        if (AuthManager.isSignedIn(this)) {
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token -> DeviceRegistrar.register(this, token) }
        }
    }
}
