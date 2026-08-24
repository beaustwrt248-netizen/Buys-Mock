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

    private suspend fun post(path: String, payload: JSONObject, bearer: String? = null): Pair<Int,String> = withContext(Dispatchers.IO) {
        val c = (URL("${BuildConfig.SUPABASE_URL}$path").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10_000
            readTimeout = 10_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            if (bearer != null) setRequestProperty("Authorization", "Bearer $bearer")
        }
        try {
            c.outputStream.use { it.write(payload.toString().toByteArray()) }
            val code = c.responseCode
            val body = (if (code in 200..299) c.inputStream else c.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            code to body
        } finally { c.disconnect() }
    }

    private fun errorMessage(body: String, fallback: String): String =
        runCatching {
            val j = JSONObject(body)
            j.optString("msg").ifBlank { j.optString("message") }.ifBlank { j.optString("error_description") }
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: fallback

    suspend fun signIn(context: Context, email: String, password: String) {
        require(email.isNotBlank()) { "Enter your email address." }
        require(password.isNotBlank()) { "Enter your password." }
        val (code, body) = post("/auth/v1/token?grant_type=password", JSONObject().apply {
            put("email", email.trim())
            put("password", password)
        })
        if (code !in 200..299) throw IllegalStateException(errorMessage(body, "Sign in failed ($code)."))
        val token = JSONObject(body).optString("access_token")
        if (token.isBlank()) throw IllegalStateException("Sign in did not return a session.")
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(ACCESS_TOKEN, token)
            .putString(USER_EMAIL, email.trim())
            .apply()
    }

    suspend fun signUp(email: String, password: String) {
        require(email.isNotBlank()) { "Enter your email address." }
        require(password.length >= 8) { "Password must be at least 8 characters." }
        val (code, body) = post("/auth/v1/signup", JSONObject().apply {
            put("email", email.trim())
            put("password", password)
            put("data", JSONObject().apply { put("full_name", email.substringBefore('@')) })
        })
        if (code !in 200..299) throw IllegalStateException(errorMessage(body, "Sign up failed ($code)."))
    }

    suspend fun sendPasswordReset(email: String) {
        require(email.isNotBlank()) { "Enter your email address." }
        val (code, body) = post("/auth/v1/recover", JSONObject().apply { put("email", email.trim()) })
        if (code !in 200..299) throw IllegalStateException(errorMessage(body, "Password reset failed ($code)."))
    }
}
