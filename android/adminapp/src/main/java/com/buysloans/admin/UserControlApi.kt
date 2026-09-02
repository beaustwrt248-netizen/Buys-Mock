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

internal object UserControlApi {
    suspend fun updateAccess(
        session: AdminSession,
        targetUserId: String,
        action: AdminUserAction,
        requestedRole: String? = null
    ) = withContext(Dispatchers.IO) {
        val decision = UserGovernancePolicy.canMutate(
            actorRole = session.role,
            actorUserId = session.userId,
            targetUserId = targetUserId,
            action = action,
            requestedRole = requestedRole
        )
        require(decision.allowed) { decision.reason }

        val payload = requestPayload(targetUserId, action, requestedRole).toString()
        val json = postAdminUserControl(session, payload, "User access could not be updated.")
        require(json.optBoolean("ok")) { "User access update was not confirmed." }
    }

    suspend fun resetPassword(session: AdminSession, targetUserId: String): AdminPasswordResetResult = withContext(Dispatchers.IO) {
        require(session.role == "admin") { "Administrator access is required." }
        require(targetUserId.isNotBlank() && targetUserId != session.userId) { "Choose another user account." }
        val payload = JSONObject()
            .put("action", "reset_password")
            .put("target_user_id", targetUserId)
            .toString()
        val json = postAdminUserControl(session, payload, "Password reset failed.")
        require(json.optBoolean("ok")) { json.optString("error").ifBlank { "Password reset was not confirmed." } }
        val password = json.optString("temporary_password")
        require(password.isNotBlank()) { "Temporary password was not returned." }
        AdminPasswordResetResult(password, json.optBoolean("requires_password_change", true))
    }

    private fun postAdminUserControl(session: AdminSession, payload: String, fallbackError: String): JSONObject {
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
            connection.outputStream.use { it.write(payload.toByteArray()) }
            val code = connection.responseCode
            val response = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            val json = runCatching { JSONObject(response) }.getOrElse { JSONObject() }
            if (code !in 200..299) {
                error(json.optString("error").ifBlank { fallbackError })
            }
            return json
        } finally {
            connection.disconnect()
        }
    }

    internal fun requestPayload(
        targetUserId: String,
        action: AdminUserAction,
        requestedRole: String? = null
    ): JSONObject {
        val payload = JSONObject()
            .put("action", UserGovernancePolicy.androidActionName(action))
            .put("target_user_id", targetUserId)
        if (action == AdminUserAction.SET_ROLE) {
            payload.put("role", requestedRole?.trim()?.lowercase())
        }
        return payload
    }
}
