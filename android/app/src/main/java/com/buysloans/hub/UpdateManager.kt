package com.buysloans.hub

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Lightweight OTA coordinator. Release metadata lives in ota/latest.json.
 * Installation remains Android/user approved; this app never silently installs APKs.
 */
data class AppUpdate(val versionCode:Int,val versionName:String,val apkUrl:String,val notes:String)

object UpdateManager {
    const val METADATA_URL = "https://raw.githubusercontent.com/beaustwrt248-netizen/Buys-Mock/native-compose/ota/latest.json"

    suspend fun check(): AppUpdate? = withContext(Dispatchers.IO) {
        val c=(URL(METADATA_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout=10000; readTimeout=10000; requestMethod="GET"
        }
        if(c.responseCode !in 200..299) return@withContext null
        val o=JSONObject(c.inputStream.bufferedReader().use{it.readText()})
        val remote=o.optInt("versionCode",0)
        if(remote<=BuildConfig.VERSION_CODE) return@withContext null
        AppUpdate(remote,o.optString("versionName"),o.optString("apkUrl"),o.optString("notes"))
    }

    fun openInstallerPermission(context:Context) {
        context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")))
    }

    fun openDownload(context:Context, update:AppUpdate) {
        // Browser/download-manager handoff is intentionally user visible. Once downloaded,
        // Android's package installer verifies package identity/signature and asks for approval.
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(update.apkUrl)))
    }
}
