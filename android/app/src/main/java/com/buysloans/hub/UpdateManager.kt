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

data class AppUpdate(
    val versionCode:Int,
    val versionName:String,
    val apkUrl:String,
    val notes:String,
    val sha256:String
)

object UpdateManager {
    const val METADATA_URL = "https://raw.githubusercontent.com/beaustwrt248-netizen/Buys-Mock/main/ota/latest.json"
    private const val RELEASE_PREFIX = "https://github.com/beaustwrt248-netizen/Buys-Mock/releases/download/"

    internal fun isTrustedApkUrl(url:String):Boolean = url.startsWith(RELEASE_PREFIX) && runCatching {
        val parsed=URL(url)
        parsed.protocol=="https" && parsed.host=="github.com" && parsed.path.startsWith("/beaustwrt248-netizen/Buys-Mock/releases/download/")
    }.getOrDefault(false)

    internal fun isValidSha256(value:String):Boolean = value.matches(Regex("^[a-fA-F0-9]{64}$"))

    suspend fun check(): AppUpdate? = withContext(Dispatchers.IO) {
        if (!OtaFeaturePolicy.isEnabled()) return@withContext null
        val cacheBustedUrl = "$METADATA_URL?t=${System.currentTimeMillis()}"
        val c=(URL(cacheBustedUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout=10000
            readTimeout=10000
            requestMethod="GET"
            useCaches=false
            instanceFollowRedirects=true
            setRequestProperty("Cache-Control","no-cache, no-store, max-age=0")
            setRequestProperty("Pragma","no-cache")
        }
        try {
            if(c.responseCode !in 200..299) throw IllegalStateException("Update server returned HTTP ${c.responseCode}")
            val o=JSONObject(c.inputStream.bufferedReader().use{it.readText()})
            val remote=o.optInt("versionCode",0)
            val name=o.optString("versionName","").trim()
            val apk=o.optString("apkUrl","").trim()
            val sha=o.optString("sha256","").trim().lowercase()
            if(remote<=0) throw IllegalStateException("Update metadata is missing versionCode")
            if(name.isBlank()) throw IllegalStateException("Update metadata is missing versionName")
            if(apk.isBlank()) throw IllegalStateException("Update metadata is missing apkUrl")
            if(!isTrustedApkUrl(apk)) throw IllegalStateException("Update download URL is not trusted")
            if(remote<=BuildConfig.VERSION_CODE) return@withContext null
            if(!isValidSha256(sha)) throw IllegalStateException("Update metadata is missing a valid SHA-256 checksum")
            AppUpdate(remote,name,apk,o.optString("notes"),sha)
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
            putExtra("sha256", update.sha256)
        })
    }
}
