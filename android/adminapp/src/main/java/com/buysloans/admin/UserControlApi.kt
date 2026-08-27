package com.buysloans.admin

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

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
            if (code !in 200..299) {
                val message = runCatching { JSONObject(response).optString("error") }.getOrNull().orEmpty()
                error(message.ifBlank { "User access could not be updated." })
            }
            val json = runCatching { JSONObject(response) }.getOrNull()
            require(json?.optBoolean("ok") == true) { "User access update was not confirmed." }
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
