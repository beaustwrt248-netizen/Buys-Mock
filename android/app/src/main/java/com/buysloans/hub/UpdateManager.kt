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
        val cacheBustedUrl = "$METADATA_URL?t=${System.currentTimeMillis()}"
        val c=(URL(cacheBustedUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout=10000
            readTimeout=10000
            requestMethod="GET"
            useCaches=false
            setRequestProperty("Cache-Control","no-cache, no-store, max-age=0")
            setRequestProperty("Pragma","no-cache")
        }
        try {
            if(c.responseCode !in 200..299) throw IllegalStateException("Update server returned HTTP ${c.responseCode}")
            val o=JSONObject(c.inputStream.bufferedReader().use{it.readText()})
            val remote=o.optInt("versionCode",0)
            val apk=o.optString("apkUrl","")
            if(remote<=0) throw IllegalStateException("Update metadata is missing versionCode")
            if(apk.isBlank()) throw IllegalStateException("Update metadata is missing apkUrl")
            if(remote<=BuildConfig.VERSION_CODE) return@withContext null
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
