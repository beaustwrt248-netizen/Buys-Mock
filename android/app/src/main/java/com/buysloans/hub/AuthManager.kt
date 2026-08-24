package com.buysloans.hub

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object AuthManager {
    private const val PREFS = "morley_auth"
    private const val ACCESS_TOKEN = "access_token"
    private const val USER_EMAIL = "user_email"

    fun isSignedIn(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(ACCESS_TOKEN, null)?.isNotBlank() == true

    fun email(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(USER_EMAIL, "").orEmpty()

    fun signOut(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }

    suspend fun signIn(context: Context, email: String, password: String) = withContext(Dispatchers.IO) {
        require(email.isNotBlank()) { "Enter your email address." }
        require(password.isNotBlank()) { "Enter your password." }
        val url = URL("${BuildConfig.SUPABASE_URL}/auth/v1/token?grant_type=password")
        val c = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10_000
            readTimeout = 10_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
        }
        try {
            val payload = JSONObject().apply {
                put("email", email.trim())
                put("password", password)
            }
            c.outputStream.use { it.write(payload.toString().toByteArray()) }
            val code = c.responseCode
            val body = (if (code in 200..299) c.inputStream else c.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                val message = runCatching { JSONObject(body).optString("msg") }.getOrNull()
                    ?.takeIf { it.isNotBlank() } ?: "Sign in failed ($code)."
                throw IllegalStateException(message)
            }
            val json = JSONObject(body)
            val token = json.optString("access_token")
            if (token.isBlank()) throw IllegalStateException("Sign in did not return a session.")
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(ACCESS_TOKEN, token)
                .putString(USER_EMAIL, email.trim())
                .apply()
        } finally { c.disconnect() }
    }
}
