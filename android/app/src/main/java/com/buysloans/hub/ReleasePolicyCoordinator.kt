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
                lastCheckedAt = System.currentTimeMillis()
                if (policy.requiresMandatoryUpdate() && !activity.isFinishing) {
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
                // Transient failures are handled by ReleasePolicyManager's verified local cache.
                // With no prior policy, the app remains usable rather than risking a bad remote lockout.
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
