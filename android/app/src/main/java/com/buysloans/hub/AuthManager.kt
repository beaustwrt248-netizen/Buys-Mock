package com.buysloans.hub

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object AuthManager {
    private const val PREFS = "morley_auth"
    private const val ACCESS_TOKEN = "access_token"
    private const val USER_EMAIL = "user_email"
    const val AUTH_CALLBACK = "bnlmorley://auth/callback"

    fun isSignedIn(context: Context): Boolean = accessToken(context).isNotBlank()

    fun accessToken(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(ACCESS_TOKEN, "").orEmpty()

    fun email(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(USER_EMAIL, "").orEmpty()

    fun signOut(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }

    private suspend fun postAbsolute(url: String, payload: JSONObject, bearer: String? = null): Pair<Int,String> = withContext(Dispatchers.IO) {
        val c = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10_000
            readTimeout = 10_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            if (!bearer.isNullOrBlank()) setRequestProperty("Authorization", "Bearer $bearer")
        }
        try {
            c.outputStream.use { it.write(payload.toString().toByteArray()) }
            val code = c.responseCode
            val body = (if (code in 200..299) c.inputStream else c.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
            code to body
        } finally { c.disconnect() }
    }

    private suspend fun post(path: String, payload: JSONObject, bearer: String? = null) =
        postAbsolute("${BuildConfig.SUPABASE_URL}$path", payload, bearer)

    private fun errorMessage(code: Int, body: String, fallback: String): String {
        val raw = runCatching {
            val j = JSONObject(body)
            j.optString("error").ifBlank { j.optString("msg") }.ifBlank { j.optString("message") }.ifBlank { j.optString("error_description") }
        }.getOrNull()?.trim().orEmpty()
        val normalized = raw.lowercase()
        return when {
            code == 429 || "rate limit" in normalized || "too many" in normalized -> "Too many attempts. Please wait a few minutes and try again."
            "email not confirmed" in normalized -> "Please confirm your email address before signing in."
            "invalid login credentials" in normalized -> "Incorrect email address or password."
            "already exists" in normalized || "already registered" in normalized -> "An account with this email already exists. Try signing in or use Forgot Password."
            raw.isNotBlank() -> raw
            else -> fallback
        }
    }

    private suspend fun verifyAuthorised(token: String): Boolean = withContext(Dispatchers.IO) {
        val url = URL("${BuildConfig.SUPABASE_URL}/rest/v1/profiles?select=is_enabled&id=eq.${URLEncoder.encode(extractUserId(token), "UTF-8")}")
        val c = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            setRequestProperty("Authorization", "Bearer $token")
        }
        try {
            if (c.responseCode !in 200..299) return@withContext false
            val arr = JSONArray(c.inputStream.bufferedReader().use { it.readText() })
            arr.length() > 0 && arr.getJSONObject(0).optBoolean("is_enabled", false)
        } finally { c.disconnect() }
    }

    private fun extractUserId(jwt: String): String = runCatching {
        val payload = jwt.split('.')[1].replace('-', '+').replace('_', '/')
        val padded = payload + "=".repeat((4 - payload.length % 4) % 4)
        JSONObject(String(android.util.Base64.decode(padded, android.util.Base64.DEFAULT))).optString("sub")
    }.getOrDefault("")

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
        if (!verifyAuthorised(token)) throw IllegalStateException("This account is not authorised for B&L Morley. Contact an administrator.")
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(ACCESS_TOKEN, token)
            .putString(USER_EMAIL, email.trim().lowercase())
            .apply()
    }

    suspend fun signUp(email: String, password: String, inviteCode: String) {
        require(email.isNotBlank()) { "Enter your approved email address." }
        require(inviteCode.isNotBlank()) { "Enter your invite code." }
        require(password.length >= 8) { "Password must be at least 8 characters." }
        val (code, body) = postAbsolute("${BuildConfig.SUPABASE_URL}/functions/v1/redeem-app-invite", JSONObject().apply {
            put("email", email.trim())
            put("password", password)
            put("inviteCode", inviteCode.trim())
        })
        if (code !in 200..299) throw IllegalStateException(errorMessage(code, body, "Invite could not be redeemed."))
    }

    suspend fun sendPasswordReset(email: String) {
        require(email.isNotBlank()) { "Enter your email address." }
        val redirect = URLEncoder.encode(AUTH_CALLBACK, Charsets.UTF_8.name())
        val (code, body) = post("/auth/v1/recover?redirect_to=$redirect", JSONObject().apply { put("email", email.trim()) })
        if (code !in 200..299) throw IllegalStateException(errorMessage(code, body, "Password reset failed ($code)."))
    }
}
