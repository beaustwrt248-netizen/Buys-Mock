package com.buysloans.admin

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

internal data class AdminPasswordResetResult(
    val temporaryPassword: String,
    val requiresPasswordChange: Boolean
)

internal object AdminPasswordResetApi {
    suspend fun reset(session: AdminSession, targetUserId: String): AdminPasswordResetResult = withContext(Dispatchers.IO) {
        require(session.role == "admin") { "Administrator access is required." }
        require(targetUserId.isNotBlank() && targetUserId != session.userId) { "Choose another user account." }
        val connection = (URL("${BuildConfig.SUPABASE_URL}/functions/v1/admin-user-control").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 20_000
            doOutput = true
            setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            setRequestProperty("Authorization", "Bearer ${session.accessToken}")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
        }
        try {
            val payload = JSONObject()
                .put("action", "reset_password")
                .put("target_user_id", targetUserId)
                .toString()
            connection.outputStream.use { it.write(payload.toByteArray()) }
            val code = connection.responseCode
            val response = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            val json = runCatching { JSONObject(response) }.getOrElse { JSONObject() }
            if (code !in 200..299 || !json.optBoolean("ok")) {
                error(json.optString("error").ifBlank { "Password reset failed." })
            }
            val password = json.optString("temporary_password")
            require(password.isNotBlank()) { "Temporary password was not returned." }
            AdminPasswordResetResult(password, json.optBoolean("requires_password_change", true))
        } finally {
            connection.disconnect()
        }
    }
}
