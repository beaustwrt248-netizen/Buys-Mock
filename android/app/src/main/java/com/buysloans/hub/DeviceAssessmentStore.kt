package com.buysloans.hub

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Instant
import java.util.UUID

object DeviceAssessmentStore {
    private const val PREFS = "morley_ai_scan_pending"
    private const val KEY_PENDING = "pending"
    private val checkpointScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val checkpoints = setOf(
        "capture_started", "front_captured", "rear_captured", "analysis_started", "analysis_failed",
        "review_ready", "review_completed", "pricing_started", "pricing_ready", "repair_decision_ready",
        "staff_confirmed", "stock_prepared", "cancelled", "completed"
    )
    private val blockedMetadataKeys = Regex("^(imei|imei\\d*|serial|serialnumber|serial_number|rawidentifier|raw_identifier|deviceidentifier|device_identifier|access[_-]?token|refresh[_-]?token|password|secret)$", RegexOption.IGNORE_CASE)

    fun beginLocalScan(context: Context): String {
        val id = UUID.randomUUID().toString()
        savePending(context, id, "capture_started", JSONObject(), null, remoteCreated = false)
        return id
    }

    fun checkpointAsync(
        context: Context,
        assessmentId: String,
        checkpoint: String,
        metadata: JSONObject = JSONObject(),
        errorCode: String? = null
    ) {
        val appContext = context.applicationContext
        checkpointScope.launch {
            runCatching {
                checkpoint(appContext, assessmentId, checkpoint, metadata, errorCode)
            }
        }
    }

    suspend fun checkpoint(
        context: Context,
        assessmentId: String,
        checkpoint: String,
        metadata: JSONObject = JSONObject(),
        errorCode: String? = null
    ): Boolean {
        require(assessmentId.isNotBlank()) { "Assessment ID is required." }
        require(checkpoint in checkpoints) { "Unsupported scan checkpoint." }
        val existing = pending(context).optJSONObject(assessmentId)
        val remoteCreated = existing?.optBoolean("remoteCreated", false) == true
        savePending(context, assessmentId, checkpoint, sanitize(metadata), errorCode?.take(120), remoteCreated)
        return runCatching { syncOne(context, assessmentId) }.getOrDefault(false)
    }

    suspend fun syncPending(context: Context): Int {
        val snapshot = pending(context)
        val ids = buildList {
            val keys = snapshot.keys()
            while (keys.hasNext()) add(keys.next())
        }
        var synced = 0
        for (id in ids) if (runCatching { syncOne(context, id) }.getOrDefault(false)) synced += 1
        return synced
    }

    fun hasPending(context: Context): Boolean = pending(context).length() > 0

    private fun sanitize(input: JSONObject): JSONObject {
        val clean = JSONObject()
        val keys = input.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            if (!blockedMetadataKeys.matches(key)) clean.put(key, input.opt(key))
        }
        return clean
    }

    @Synchronized
    private fun savePending(
        context: Context,
        assessmentId: String,
        checkpoint: String,
        metadata: JSONObject,
        errorCode: String?,
        remoteCreated: Boolean
    ) {
        val all = pending(context)
        val now = Instant.now().toString()
        val item = JSONObject().apply {
            put("checkpoint", checkpoint)
            put("metadata", sanitize(metadata))
            put("lastCheckpointAt", now)
            put("remoteCreated", remoteCreated)
            if (!errorCode.isNullOrBlank()) put("errorCode", errorCode) else put("errorCode", JSONObject.NULL)
        }
        all.put(assessmentId, item)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_PENDING, all.toString()).apply()
    }

    @Synchronized
    private fun pending(context: Context): JSONObject {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_PENDING, "{}").orEmpty()
        return runCatching { JSONObject(raw) }.getOrElse { JSONObject() }
    }

    @Synchronized
    private fun removePending(context: Context, assessmentId: String) {
        val all = pending(context)
        all.remove(assessmentId)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_PENDING, all.toString()).apply()
    }

    private suspend fun syncOne(context: Context, assessmentId: String): Boolean = withContext(Dispatchers.IO) {
        val item = pending(context).optJSONObject(assessmentId) ?: return@withContext true
        val token = AuthManager.validAccessToken(context)
        val checkpoint = item.optString("checkpoint")
        if (checkpoint !in checkpoints) return@withContext false
        val now = item.optString("lastCheckpointAt").ifBlank { Instant.now().toString() }
        val payload = JSONObject().apply {
            put("checkpoint", checkpoint)
            put("checkpoint_metadata", sanitize(item.optJSONObject("metadata") ?: JSONObject()))
            put("last_checkpoint_at", now)
            if (!item.isNull("errorCode")) put("last_error_code", item.optString("errorCode").take(120)) else put("last_error_code", JSONObject.NULL)
            if (checkpoint == "cancelled") put("cancelled_at", now)
            if (checkpoint == "completed") put("completed_at", now)
        }
        var remoteCreated = item.optBoolean("remoteCreated", false)
        if (!remoteCreated) {
            val create = JSONObject(payload.toString()).apply {
                put("id", assessmentId)
                put("state", "draft")
                put("source", "device_lens")
            }
            val code = request(context, "POST", "/rest/v1/device_assessments", create, token, "return=minimal")
            if (code !in 200..299 && code != 409) return@withContext false
            remoteCreated = true
            savePending(context, assessmentId, checkpoint, item.optJSONObject("metadata") ?: JSONObject(), item.optString("errorCode").ifBlank { null }, remoteCreated)
            if (code in 200..299) {
                removePending(context, assessmentId)
                return@withContext true
            }
        }
        if (remoteCreated) {
            val encoded = URLEncoder.encode(assessmentId, Charsets.UTF_8.name())
            val code = request(context, "PATCH", "/rest/v1/device_assessments?id=eq.$encoded", payload, token, "return=minimal")
            if (code in 200..299) {
                removePending(context, assessmentId)
                return@withContext true
            }
        }
        false
    }

    private fun request(
        context: Context,
        method: String,
        path: String,
        payload: JSONObject,
        token: String,
        prefer: String
    ): Int {
        val connection = (URL("${BuildConfig.SUPABASE_URL}$path").openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 10_000
            readTimeout = 12_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Prefer", prefer)
        }
        return try {
            connection.outputStream.use { it.write(payload.toString().toByteArray()) }
            val code = connection.responseCode
            (if (code in 200..299) connection.inputStream else connection.errorStream)?.close()
            code
        } finally {
            connection.disconnect()
        }
    }
}
