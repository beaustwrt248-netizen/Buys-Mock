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

data class AppUpdate(val versionCode:Int,val versionName:String,val apkUrl:String,val notes:String)

object UpdateManager {
    const val METADATA_URL = "https://raw.githubusercontent.com/beaustwrt248-netizen/Buys-Mock/main/ota/latest.json"

    suspend fun check(): AppUpdate? = withContext(Dispatchers.IO) {
        val c=(URL(METADATA_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout=10000
            readTimeout=10000
            requestMethod="GET"
            setRequestProperty("Cache-Control","no-cache")
        }
        try {
            if(c.responseCode !in 200..299) return@withContext null
            val o=JSONObject(c.inputStream.bufferedReader().use{it.readText()})
            val remote=o.optInt("versionCode",0)
            val apk=o.optString("apkUrl","")
            if(remote<=BuildConfig.VERSION_CODE || apk.isBlank()) return@withContext null
            AppUpdate(remote,o.optString("versionName"),apk,o.optString("notes"))
        } finally { c.disconnect() }
    }

    fun openInstallerPermission(context:Context) {
        context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")))
    }

    fun openDownload(context:Context, update:AppUpdate) {
        context.startActivity(Intent(context, UpdateActivity::class.java).apply {
            putExtra("versionCode", update.versionCode)
            putExtra("versionName", update.versionName)
            putExtra("apkUrl", update.apkUrl)
            putExtra("notes", update.notes)
        })
    }
}
