package com.buysloans.hub

import android.content.Context
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object AuthManager {
    private const val PREFS = "morley_auth"
    private const val ACCESS_TOKEN = "access_token"
    private const val USER_EMAIL = "user_email"
    const val AUTH_CALLBACK = "bnlmorley://auth/callback"

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

    private fun errorMessage(code: Int, body: String, fallback: String): String {
        val raw = runCatching {
            val j = JSONObject(body)
            j.optString("msg")
                .ifBlank { j.optString("message") }
                .ifBlank { j.optString("error_description") }
                .ifBlank { j.optString("error") }
        }.getOrNull()?.trim().orEmpty()

        val normalized = raw.lowercase()
        return when {
            code == 429 || "rate limit" in normalized || "too many" in normalized ->
                "Too many verification emails have been sent. Please wait a few minutes, then try again."
            "email not confirmed" in normalized ->
                "Please confirm your email address before signing in."
            "invalid login credentials" in normalized ->
                "Incorrect email address or password."
            "user already registered" in normalized || "already been registered" in normalized ->
                "An account with this email already exists. Try signing in or use Forgot Password."
            raw.isNotBlank() -> raw
            else -> fallback
        }
    }

    suspend fun signIn(context: Context, email: String, password: String) {
        require(email.isNotBlank()) { "Enter your email address." }
        require(password.isNotBlank()) { "Enter your password." }
        val (code, body) = post("/auth/v1/token?grant_type=password", JSONObject().apply {
            put("email", email.trim())
            put("password", password)
        })
        if (code !in 200..299) throw IllegalStateException(errorMessage(code, body, "Sign in failed ($code)."))
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
        val redirect = URLEncoder.encode(AUTH_CALLBACK, Charsets.UTF_8.name())
        val (code, body) = post("/auth/v1/signup?redirect_to=$redirect", JSONObject().apply {
            put("email", email.trim())
            put("password", password)
            put("data", JSONObject().apply { put("full_name", email.substringBefore('@')) })
        })
        if (code !in 200..299) throw IllegalStateException(errorMessage(code, body, "Sign up failed ($code)."))
    }

    suspend fun sendPasswordReset(email: String) {
        require(email.isNotBlank()) { "Enter your email address." }
        val redirect = URLEncoder.encode(AUTH_CALLBACK, Charsets.UTF_8.name())
        val (code, body) = post("/auth/v1/recover?redirect_to=$redirect", JSONObject().apply { put("email", email.trim()) })
        if (code !in 200..299) throw IllegalStateException(errorMessage(code, body, "Password reset failed ($code)."))
    }
}
