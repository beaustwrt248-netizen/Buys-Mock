package com.buysloans.hub

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object ReleasePolicyCoordinator : Application.ActivityLifecycleCallbacks {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var checking = false
    private var lastCheckedAt = 0L

    fun register(application: Application) {
        application.registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityResumed(activity: Activity) {
        if (activity is AuthActivity || activity is UpdateActivity || activity is MandatoryUpdateActivity) return
        if (!AuthManager.isSignedIn(activity)) return
        val now = System.currentTimeMillis()
        if (checking || now - lastCheckedAt < 30_000L) return
        checking = true
        scope.launch {
            try {
                val policy = ReleasePolicyManager.load(activity.applicationContext)
                val update = UpdateManager.check()
                lastCheckedAt = System.currentTimeMillis()
                if ((policy.requiresMandatoryUpdate() || update != null) && !activity.isFinishing) {
                    activity.startActivity(
                        Intent(activity, MandatoryUpdateActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    )
                }
            } catch (_: SecurityException) {
                if (!AuthManager.isSignedIn(activity) && !activity.isFinishing) {
                    activity.startActivity(
                        Intent(activity, AuthActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    )
                    activity.finish()
                }
            } catch (_: Exception) {
                // Transient failures never create a false lockout. A verified remote OTA or
                // cached mandatory support policy must be available before the update gate opens.
            } finally {
                checking = false
            }
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}
