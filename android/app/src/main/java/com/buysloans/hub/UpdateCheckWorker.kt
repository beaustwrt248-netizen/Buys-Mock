package com.buysloans.hub

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class UpdateCheckWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val update = UpdateManager.check() ?: return Result.success()
            val prefs = applicationContext.getSharedPreferences("update_alerts", Context.MODE_PRIVATE)
            val lastNotified = prefs.getInt("last_notified_version_code", 0)

            if (update.versionCode > lastNotified) {
                NotificationHelper.createChannels(applicationContext)
                val delivered = NotificationHelper.showUpdateAvailable(applicationContext, update)
                if (delivered) {
                    prefs.edit().putInt("last_notified_version_code", update.versionCode).apply()
                }
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}

object UpdateCheckScheduler {
    private const val PERIODIC_WORK = "bl_morley_periodic_update_check"
    private const val IMMEDIATE_WORK = "bl_morley_launch_update_check"

    fun schedule(context: Context) {
        val workManager = WorkManager.getInstance(context)

        val periodic = PeriodicWorkRequestBuilder<UpdateCheckWorker>(12, TimeUnit.HOURS)
            .build()
        workManager.enqueueUniquePeriodicWork(
            PERIODIC_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodic
        )

        val immediate = OneTimeWorkRequestBuilder<UpdateCheckWorker>().build()
        workManager.enqueueUniqueWork(
            IMMEDIATE_WORK,
            ExistingWorkPolicy.REPLACE,
            immediate
        )
    }
}
