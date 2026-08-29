package com.buysloans.hub

import android.app.Application
import com.google.firebase.messaging.FirebaseMessaging

class MorleyApplication : Application() {
    companion object {
        lateinit var instance: MorleyApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        AppCopyrightFooter.install(this)
        NotificationHelper.createChannels(this)
        UpdateCheckScheduler.schedule(this)
        ReleasePolicyCoordinator.register(this)
        if (AuthManager.isSignedIn(this)) {
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token -> DeviceRegistrar.register(this, token) }
        }
    }
}
