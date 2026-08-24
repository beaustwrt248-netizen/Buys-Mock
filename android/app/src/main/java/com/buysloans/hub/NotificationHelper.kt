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
    const val CHANNEL_ADMIN = "admin_messages"

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
        val admin = NotificationChannel(
            CHANNEL_ADMIN,
            "B&L Morley messages",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Messages and alerts sent from B&L Morley Admin Control"
        }
        manager.createNotificationChannels(listOf(updates, valuations, admin))
    }

    private fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun showRemoteMessage(context: Context, title: String, body: String): Boolean {
        if (!canNotify(context)) return false
        val launchIntent = Intent(context, DashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            3001,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = Notification.Builder(context, CHANNEL_ADMIN)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(Notification.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java)
            .notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
        return true
    }

    fun showUpdateAvailable(context: Context, update: AppUpdate): Boolean {
        if (!canNotify(context)) return false

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
        return true
    }
}
