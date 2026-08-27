package com.buysloans.hub

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class ReleaseSupportPolicy(
    val minimumVersionCode: Int,
    val minimumVersionName: String,
    val forceUpdate: Boolean,
    val checkedAt: Long
) {
    fun isBelowMinimum(): Boolean = minimumVersionCode > 0 && BuildConfig.VERSION_CODE < minimumVersionCode
    fun requiresMandatoryUpdate(): Boolean = forceUpdate && isBelowMinimum()
}

object ReleasePolicyManager {
    private const val PREFS = "morley_release_policy"
    private const val KEY_POLICY = "last_verified_policy"

    private class PolicyHttpException(val status: Int) : IOException("Release policy server returned HTTP $status")

    suspend fun load(context: Context): ReleaseSupportPolicy {
        val token = AuthManager.validAccessToken(context)
        return try {
            fetch(token).also { save(context, it) }
        } catch (error: PolicyHttpException) {
            if (error.status == 429 || error.status >= 500) cached(context) ?: throw error
            else throw error
        } catch (error: IOException) {
            cached(context) ?: throw error
        }
    }

    fun cached(context: Context): ReleaseSupportPolicy? {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_POLICY, "")
            .orEmpty()
        if (raw.isBlank()) return null
        return runCatching {
            val json = JSONObject(raw)
            ReleaseSupportPolicy(
                minimumVersionCode = json.optInt("minimumVersionCode", 0),
                minimumVersionName = json.optString("minimumVersionName"),
                forceUpdate = json.optBoolean("forceUpdate", false),
                checkedAt = json.optLong("checkedAt", 0L)
            )
        }.getOrNull()
    }

    private suspend fun fetch(token: String): ReleaseSupportPolicy = withContext(Dispatchers.IO) {
        val url = URL("${BuildConfig.SUPABASE_URL}/rest/v1/app_config?select=key,value&key=eq.minimum_supported_version")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Cache-Control", "no-cache")
        }
        try {
            val code = connection.responseCode
            if (code == 401 || code == 403) throw SecurityException("Release policy access was denied.")
            if (code !in 200..299) throw PolicyHttpException(code)
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val rows = JSONArray(body)
            if (rows.length() == 0) return@withContext ReleaseSupportPolicy(0, "", false, System.currentTimeMillis())
            val value = rows.optJSONObject(0)?.optJSONObject("value") ?: JSONObject()
            val minimumCode = value.optInt("versionCode", 0).coerceAtLeast(0)
            val minimumName = value.optString("versionName").trim()
            val force = value.optBoolean("forceUpdate", false)
            if (force && minimumCode <= 0) throw IOException("Forced update policy is missing a valid minimum version.")
            ReleaseSupportPolicy(minimumCode, minimumName, force, System.currentTimeMillis())
        } finally {
            connection.disconnect()
        }
    }

    private fun save(context: Context, policy: ReleaseSupportPolicy) {
        val json = JSONObject()
            .put("minimumVersionCode", policy.minimumVersionCode)
            .put("minimumVersionName", policy.minimumVersionName)
            .put("forceUpdate", policy.forceUpdate)
            .put("checkedAt", policy.checkedAt)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_POLICY, json.toString())
            .apply()
    }
}
