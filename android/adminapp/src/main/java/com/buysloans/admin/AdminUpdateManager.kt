package com.buysloans.admin

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

internal object AdminUpdateManager {
    const val METADATA_URL = "https://beaustwrt248-netizen.github.io/Buys-Mock/admin/admin-update.json"

    suspend fun fetchRelease(): AdminUpdateRelease = withContext(Dispatchers.IO) {
        val connection = (URL(METADATA_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 10_000
            instanceFollowRedirects = true
        }
        try {
            require(connection.responseCode in 200..299) { "Update metadata request failed (${connection.responseCode})." }
            parseRelease(connection.inputStream.bufferedReader().use { it.readText() })
        } finally {
            connection.disconnect()
        }
    }

    internal fun parseRelease(json: String): AdminUpdateRelease {
        val obj = JSONObject(json)
        return AdminUpdateRelease(
            versionCode = obj.getInt("versionCode"),
            versionName = obj.getString("versionName"),
            apkUrl = obj.getString("apkUrl"),
            sha256 = obj.getString("sha256"),
            required = obj.optBoolean("required", false)
        )
    }

    suspend fun downloadVerifiedApk(context: Context, release: AdminUpdateRelease): File = withContext(Dispatchers.IO) {
        require(shouldOfferAdminUpdate(BuildConfig.VERSION_CODE, release)) { "Release is not eligible for installation." }
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        val target = File(dir, "Morley-Admin-update.apk")
        val connection = (URL(release.apkUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            instanceFollowRedirects = true
        }
        try {
            require(connection.responseCode in 200..299) { "Update download failed (${connection.responseCode})." }
            connection.inputStream.use { input -> target.outputStream().use { output -> input.copyTo(output) } }
        } finally {
            connection.disconnect()
        }
        val actual = target.inputStream().use { input ->
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        }
        require(actual.equals(release.sha256, ignoreCase = true)) { "Downloaded update failed SHA-256 verification." }
        target
    }

    fun launchInstaller(context: Context, apk: File): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
            context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            return false
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", apk)
        context.startActivity(Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        })
        return true
    }
}
