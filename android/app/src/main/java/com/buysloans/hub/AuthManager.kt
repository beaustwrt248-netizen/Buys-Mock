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
    private const val REFRESH_TOKEN = "refresh_token"
    private const val EXPIRES_AT = "expires_at"
    private const val USER_EMAIL = "user_email"
    private const val DISPLAY_NAME = "display_name"
    private const val USER_ROLE = "user_role"
    const val AUTH_CALLBACK = "bnlmorley://auth/callback"

    fun accessToken(context: Context): String = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(ACCESS_TOKEN, "").orEmpty()
    fun refreshToken(context: Context): String = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(REFRESH_TOKEN, "").orEmpty()
    fun email(context: Context): String = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(USER_EMAIL, "").orEmpty()
    fun displayName(context: Context): String = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(DISPLAY_NAME, "").orEmpty()
    fun role(context: Context): String = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(USER_ROLE, "user").orEmpty().lowercase().ifBlank { "user" }
    fun canUseAdminMode(context: Context): Boolean = AdminModePolicy.canEnter(role(context))
    fun accountLabel(context: Context): String = displayName(context).ifBlank { email(context) }.ifBlank { "Authorised B&L Morley account" }
    private fun expiresAt(context: Context): Long = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(EXPIRES_AT, 0L)

    fun isSignedIn(context: Context): Boolean = accessToken(context).isNotBlank() || refreshToken(context).isNotBlank()

    fun signOut(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }

    private suspend fun requestAbsolute(url: String, method: String, payload: JSONObject? = null, bearer: String? = null): Pair<Int,String> = withContext(Dispatchers.IO) {
        val c = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method; connectTimeout = 10_000; readTimeout = 10_000
            doOutput = payload != null
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            if (!bearer.isNullOrBlank()) setRequestProperty("Authorization", "Bearer $bearer")
        }
        try {
            if (payload != null) c.outputStream.use { it.write(payload.toString().toByteArray()) }
            val code = c.responseCode
            val body = (if (code in 200..299) c.inputStream else c.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
            code to body
        } finally { c.disconnect() }
    }

    private suspend fun postAbsolute(url: String, payload: JSONObject, bearer: String? = null): Pair<Int,String> =
        requestAbsolute(url, "POST", payload, bearer)

    private suspend fun post(path: String, payload: JSONObject, bearer: String? = null) = postAbsolute("${BuildConfig.SUPABASE_URL}$path", payload, bearer)

    private fun withCaptcha(payload: JSONObject, captchaToken: String): JSONObject {
        require(captchaToken.isNotBlank()) { "Complete the security check first." }
        payload.put("gotrue_meta_security", JSONObject().put("captcha_token", captchaToken))
        return payload
    }

    private fun errorMessage(code: Int, body: String, fallback: String): String {
        val raw = runCatching { val j = JSONObject(body); j.optString("error").ifBlank { j.optString("msg") }.ifBlank { j.optString("message") }.ifBlank { j.optString("error_description") } }.getOrNull()?.trim().orEmpty()
        val normalized = raw.lowercase()
        return when {
            code == 429 || "rate limit" in normalized || "too many" in normalized -> "Too many attempts. Please wait a few minutes and try again."
            "captcha" in normalized -> "Security check failed or expired. Complete it again and retry."
            "email not confirmed" in normalized -> "Please confirm your email address before signing in."
            "invalid login credentials" in normalized -> "Incorrect email address or password."
            "already exists" in normalized || "already registered" in normalized -> "An account with this email already exists. Try signing in or use Forgot Password."
            raw.isNotBlank() -> raw
            else -> fallback
        }
    }

    private suspend fun authorisedProfile(token: String): JSONObject? = withContext(Dispatchers.IO) {
        val userId = extractUserId(token); if (userId.isBlank()) return@withContext null
        val url = URL("${BuildConfig.SUPABASE_URL}/rest/v1/profiles?select=is_enabled,display_name,email,role&id=eq.${URLEncoder.encode(userId, "UTF-8")}")
        val c = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"; connectTimeout = 10_000; readTimeout = 10_000
            setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY); setRequestProperty("Authorization", "Bearer $token")
        }
        try {
            if (c.responseCode !in 200..299) return@withContext null
            val arr = JSONArray(c.inputStream.bufferedReader().use { it.readText() })
            if (arr.length() == 0) null else arr.getJSONObject(0).takeIf { it.optBoolean("is_enabled", false) }
        } finally { c.disconnect() }
    }

    private suspend fun verifyAndCacheProfile(context: Context, token: String): Boolean {
        val profile = authorisedProfile(token) ?: return false
        val name = profile.optString("display_name").trim()
        val profileEmail = profile.optString("email").trim().lowercase()
        val profileRole = profile.optString("role", "user").trim().lowercase().ifBlank { "user" }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().apply {
            if (name.isNotBlank()) putString(DISPLAY_NAME, name)
            if (profileEmail.isNotBlank()) putString(USER_EMAIL, profileEmail)
            putString(USER_ROLE, profileRole)
        }.apply()
        return true
    }

    suspend fun updateDisplayName(context: Context, newName: String) {
        val name = newName.trim().replace(Regex("\\s+"), " ")
        require(name.length >= 3 && name.contains(' ')) { "Enter your first and last name." }
        val token = validAccessToken(context)
        val userId = extractUserId(token)
        require(userId.isNotBlank()) { "Could not identify the signed-in account." }
        val url = "${BuildConfig.SUPABASE_URL}/rest/v1/profiles?id=eq.${URLEncoder.encode(userId, "UTF-8")}"
        val (code, body) = requestAbsolute(url, "PATCH", JSONObject().put("display_name", name), token)
        if (code !in 200..299) throw IllegalStateException(errorMessage(code, body, "Profile update failed ($code)."))
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(DISPLAY_NAME, name).apply()
    }

    suspend fun signOutEverywhere(context: Context) {
        val token = runCatching { validAccessToken(context) }.getOrNull()
        if (!token.isNullOrBlank()) {
            val (code, body) = requestAbsolute("${BuildConfig.SUPABASE_URL}/auth/v1/logout?scope=global", "POST", JSONObject(), token)
            if (code !in 200..299 && code != 401) throw IllegalStateException(errorMessage(code, body, "Could not sign out all sessions ($code)."))
        }
        signOut(context)
    }

    private fun extractUserId(jwt: String): String = runCatching {
        val payload = jwt.split('.')[1].replace('-', '+').replace('_', '/')
        val padded = payload + "=".repeat((4 - payload.length % 4) % 4)
        JSONObject(String(android.util.Base64.decode(padded, android.util.Base64.DEFAULT))).optString("sub")
    }.getOrDefault("")

    private fun saveSession(context: Context, body: String, fallbackEmail: String = email(context)) {
        val j = JSONObject(body)
        val access = j.optString("access_token")
        val refresh = j.optString("refresh_token")
        val expiresIn = j.optLong("expires_in", 3600L)
        require(access.isNotBlank()) { "Authentication did not return a session." }
        val edit = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(ACCESS_TOKEN, access)
            .putLong(EXPIRES_AT, System.currentTimeMillis() / 1000L + expiresIn)
        if (refresh.isNotBlank()) edit.putString(REFRESH_TOKEN, refresh)
        if (fallbackEmail.isNotBlank()) edit.putString(USER_EMAIL, fallbackEmail.trim().lowercase())
        edit.apply()
    }

    suspend fun validAccessToken(context: Context): String {
        val access = accessToken(context)
        val expiry = expiresAt(context)
        val now = System.currentTimeMillis() / 1000L
        if (access.isNotBlank() && (expiry == 0L || expiry > now + 60L)) {
            if (!verifyAndCacheProfile(context, access)) {
                signOut(context)
                throw IllegalStateException("This account is no longer authorised for B&L Morley.")
            }
            return access
        }
        val refresh = refreshToken(context)
        if (refresh.isBlank()) {
            signOut(context)
            throw IllegalStateException("Your session has expired. Please sign in again once to enable automatic session renewal.")
        }
        val (code, body) = post("/auth/v1/token?grant_type=refresh_token", JSONObject().put("refresh_token", refresh))
        if (code !in 200..299) {
            signOut(context)
            throw IllegalStateException("Your session has expired. Please sign in again.")
        }
        saveSession(context, body)
        val token = accessToken(context)
        if (!verifyAndCacheProfile(context, token)) {
            signOut(context)
            throw IllegalStateException("This account is no longer authorised for B&L Morley.")
        }
        return token
    }

    @Deprecated("Use the CAPTCHA-protected signIn overload")
    suspend fun signIn(context: Context, email: String, password: String) {
        throw IllegalStateException("Secure sign-in is required. Return to the B&L Morley sign-in screen and complete the security check.")
    }

    suspend fun signIn(context: Context, email: String, password: String, captchaToken: String) {
        require(email.isNotBlank()) { "Enter your email address." }; require(password.isNotBlank()) { "Enter your password." }
        val payload = withCaptcha(JSONObject().apply { put("email", email.trim()); put("password", password) }, captchaToken)
        val (code, body) = post("/auth/v1/token?grant_type=password", payload)
        if (code !in 200..299) throw IllegalStateException(errorMessage(code, body, "Sign in failed ($code)."))
        val token = JSONObject(body).optString("access_token")
        if (token.isBlank()) throw IllegalStateException("Sign in did not return a session.")
        saveSession(context, body, email)
        if (!verifyAndCacheProfile(context, token)) {
            signOut(context)
            throw IllegalStateException("This account is not authorised for B&L Morley. Contact an administrator.")
        }
    }

    suspend fun signUp(email: String, password: String, inviteCode: String, captchaToken: String) {
        require(email.isNotBlank()) { "Enter your approved email address." }; require(inviteCode.isNotBlank()) { "Enter your invite code." }; require(password.length >= 10) { "Password must be at least 10 characters." }; require(captchaToken.isNotBlank()) { "Complete the security check first." }
        val (code, body) = postAbsolute("${BuildConfig.SUPABASE_URL}/functions/v1/redeem-app-invite", JSONObject().apply { put("email", email.trim()); put("password", password); put("inviteCode", inviteCode.trim()); put("captchaToken", captchaToken) })
        if (code !in 200..299) throw IllegalStateException(errorMessage(code, body, "Invite could not be redeemed."))
    }

    suspend fun sendPasswordReset(email: String, captchaToken: String) {
        require(email.isNotBlank()) { "Enter your email address." }
        val redirect = URLEncoder.encode(AUTH_CALLBACK, Charsets.UTF_8.name())
        val payload = withCaptcha(JSONObject().apply { put("email", email.trim()) }, captchaToken)
        val (code, body) = post("/auth/v1/recover?redirect_to=$redirect", payload)
        if (code !in 200..299) throw IllegalStateException(errorMessage(code, body, "Password reset failed ($code)."))
    }
}
