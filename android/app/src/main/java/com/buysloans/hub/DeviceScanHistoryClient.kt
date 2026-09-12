package com.buysloans.hub

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class DeviceScanHistoryRow(
    val id: String,
    val checkpoint: String,
    val state: String,
    val model: String,
    val storageGb: Int?,
    val confidence: Double?,
    val errorCode: String?,
    val createdAt: String,
    val updatedAt: String
) {
    val displayName: String
        get() = buildString {
            append(model.ifBlank { "Unidentified device" })
            storageGb?.let { append(" • ${it}GB") }
        }

    val isCompleted: Boolean get() = checkpoint == "completed"
    val isFailed: Boolean get() = checkpoint == "analysis_failed"
    val isCancelled: Boolean get() = checkpoint == "cancelled"
    val canResume: Boolean get() = !isCompleted
}

object DeviceScanHistoryClient {
    suspend fun list(context: Context, limit: Int = 75): List<DeviceScanHistoryRow> = withContext(Dispatchers.IO) {
        val token = AuthManager.validAccessToken(context)
        val bounded = limit.coerceIn(1, 100)
        val select = URLEncoder.encode(
            "id,state,checkpoint,resolved_model,resolved_storage_gb,identity_confidence,last_error_code,created_at,updated_at",
            Charsets.UTF_8.name()
        )
        val path = "/rest/v1/device_assessments?select=$select&source=eq.device_lens&order=created_at.desc&limit=$bounded"
        val connection = (URL("${BuildConfig.SUPABASE_URL}$path").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 15_000
            setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Accept", "application/json")
        }
        try {
            val code = connection.responseCode
            val body = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                val message = runCatching { JSONObject(body).optString("message") }.getOrNull().orEmpty()
                error(message.ifBlank { "AI scan history could not be loaded." })
            }
            val array = JSONArray(body.ifBlank { "[]" })
            buildList {
                for (index in 0 until array.length()) {
                    val row = array.optJSONObject(index) ?: continue
                    add(
                        DeviceScanHistoryRow(
                            id = row.optString("id"),
                            state = row.optString("state", "draft"),
                            checkpoint = row.optString("checkpoint", "capture_started"),
                            model = row.optString("resolved_model"),
                            storageGb = if (row.isNull("resolved_storage_gb")) null else row.optInt("resolved_storage_gb"),
                            confidence = if (row.isNull("identity_confidence")) null else row.optDouble("identity_confidence"),
                            errorCode = row.optString("last_error_code").takeIf { it.isNotBlank() },
                            createdAt = row.optString("created_at"),
                            updatedAt = row.optString("updated_at")
                        )
                    )
                }
            }
        } finally {
            connection.disconnect()
        }
    }
}
