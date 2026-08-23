package com.buysloans.hub

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

object NotificationHelper {
    const val CHANNEL_UPDATES = "updates"
    const val CHANNEL_VALUATIONS = "valuations"

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val updates = NotificationChannel(
            CHANNEL_UPDATES,
            "App updates",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "New B&L Morley versions and update status"
        }
        val valuations = NotificationChannel(
            CHANNEL_VALUATIONS,
            "Valuation alerts",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Pricing and valuation alerts from B&L Morley"
        }
        manager.createNotificationChannels(listOf(updates, valuations))
    }

    fun showUpdateAvailable(context: Context, update: AppUpdate) {
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val launchIntent = Intent(context, UpdateActivity::class.java).apply {
            putExtra("versionCode", update.versionCode)
            putExtra("versionName", update.versionName)
            putExtra("apkUrl", update.apkUrl)
            putExtra("notes", update.notes)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            2001,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = Notification.Builder(context, CHANNEL_UPDATES)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("B&L Morley update available")
            .setContentText("Version ${update.versionName} is ready to download and install.")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        context.getSystemService(NotificationManager::class.java).notify(2001, notification)
    }
}
