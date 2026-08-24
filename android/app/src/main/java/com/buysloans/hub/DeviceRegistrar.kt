package com.buysloans.hub

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import java.util.concurrent.Executors

object DeviceRegistrar {
    private const val PREFS = "morley_device"
    private const val KEY_INSTALLATION_ID = "installation_id"
    private val executor = Executors.newSingleThreadExecutor()

    fun register(context: Context, fcmToken: String) {
        if (fcmToken.isBlank()) return
        val appContext = context.applicationContext
        executor.execute {
            runCatching {
                val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                val installationId = prefs.getString(KEY_INSTALLATION_ID, null)
                    ?: UUID.randomUUID().toString().also {
                        prefs.edit().putString(KEY_INSTALLATION_ID, it).apply()
                    }

                val pm = appContext.packageManager
                val packageInfo = pm.getPackageInfo(appContext.packageName, 0)
                val versionName = packageInfo.versionName ?: BuildConfig.VERSION_NAME
                val versionCode = if (Build.VERSION.SDK_INT >= 28) {
                    packageInfo.longVersionCode.toInt()
                } else {
                    @Suppress("DEPRECATION") packageInfo.versionCode
                }
                val notificationsEnabled = Build.VERSION.SDK_INT < 33 ||
                    appContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

                val payload = JSONObject().apply {
                    put("p_installation_id", installationId)
                    put("p_device_name", "${Build.MANUFACTURER} ${Build.MODEL}".trim())
                    put("p_app_version", versionName)
                    put("p_app_version_code", versionCode)
                    put("p_fcm_token", fcmToken)
                    put("p_notifications_enabled", notificationsEnabled)
                }

                val url = URL("${BuildConfig.SUPABASE_URL}/rest/v1/rpc/register_device_public")
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 10_000
                    readTimeout = 10_000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
                    setRequestProperty("Authorization", "Bearer ${BuildConfig.SUPABASE_PUBLISHABLE_KEY}")
                }
                connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
                val code = connection.responseCode
                if (code !in 200..299) {
                    val body = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                    throw IllegalStateException("Device registration failed ($code): $body")
                }
                connection.inputStream?.close()
                connection.disconnect()
            }
        }
    }
}
