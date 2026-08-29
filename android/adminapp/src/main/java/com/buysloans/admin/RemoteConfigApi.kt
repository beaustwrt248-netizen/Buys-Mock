package com.buysloans.admin

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

internal object RemoteConfigApi {
    suspend fun update(
        session: AdminSession,
        current: MaintenanceConfig,
        maintenanceEnabled: Boolean,
        maintenanceMessage: String,
        otaEnabled: Boolean
    ) = withContext(Dispatchers.IO) {
        val payload = maintenanceUpdatePayload(
            current = current,
            enabled = maintenanceEnabled,
            message = maintenanceMessage,
            otaEnabled = otaEnabled
        ).toString()
        val connection = (URL("${BuildConfig.SUPABASE_URL}/rest/v1/rpc/admin_set_config").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 20_000
            doOutput = true
            setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            setRequestProperty("Authorization", "Bearer ${session.accessToken}")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Prefer", "return=minimal")
        }
        try {
            connection.outputStream.use { it.write(payload.toByteArray()) }
            val code = connection.responseCode
            if (code !in 200..299) {
                val body = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw IllegalStateException(body.ifBlank { "Remote configuration could not be updated ($code)." })
            }
        } finally {
            connection.disconnect()
        }
    }
}
