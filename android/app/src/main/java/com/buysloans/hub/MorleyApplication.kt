package com.buysloans.hub

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MorleyApplication : Application(), Application.ActivityLifecycleCallbacks {
    companion object {
        lateinit var instance: MorleyApplication
            private set
    }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    @Volatile private var maintenanceCheckInFlight = false
    @Volatile private var lastMaintenanceCheckAt = 0L

    override fun onCreate() {
        super.onCreate()
        instance = this
        registerActivityLifecycleCallbacks(this)
        AppCopyrightFooter.install(this)
        NotificationHelper.createChannels(this)
        UpdateCheckScheduler.schedule(this)
        ReleasePolicyCoordinator.register(this)
        if (AuthManager.isSignedIn(this)) {
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token -> DeviceRegistrar.register(this, token) }
        }
    }

    private fun checkMaintenance(activity: Activity) {
        if (activity is AuthActivity || activity is MaintenanceActivity) return
        if (!AuthManager.isSignedIn(activity) || maintenanceCheckInFlight) return
        val now = System.currentTimeMillis()
        if (now - lastMaintenanceCheckAt < 10_000L) return
        maintenanceCheckInFlight = true
        lastMaintenanceCheckAt = now
        appScope.launch {
            val state = runCatching { MaintenanceModeClient.fetch(activity) }.getOrNull()
            maintenanceCheckInFlight = false
            if (state?.enabled == true && !activity.isFinishing && !activity.isDestroyed) {
                activity.startActivity(Intent(activity, MaintenanceActivity::class.java).apply {
                    putExtra(MaintenanceActivity.EXTRA_MESSAGE, state.message)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                })
                activity.finish()
            }
        }
    }

    override fun onActivityResumed(activity: Activity) = checkMaintenance(activity)
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}
