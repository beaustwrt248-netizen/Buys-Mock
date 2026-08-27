package com.buysloans.hub

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object SupportTicketClient {
    private const val ATTACHMENT_BUCKET = "support-ticket-attachments"
    private const val MAX_ATTACHMENT_BYTES = 10L * 1024L * 1024L
    private val allowedTypes = setOf("image/jpeg", "image/png", "image/webp", "application/pdf")
    private val allowedCategories = setOf("valuation", "pricing", "inventory", "scanner", "account", "update", "other")

    data class SubmitResult(val ticketId: String, val attachmentWarning: String? = null)

    suspend fun submit(
        context: Context,
        category: String,
        subject: String,
        description: String,
        includeDiagnostics: Boolean,
        attachment: Uri? = null
    ): SubmitResult {
        val cleanCategory = category.trim().lowercase()
        val cleanSubject = subject.trim()
        val cleanDescription = description.trim()
        require(cleanCategory in allowedCategories) { "Choose a valid support category." }
        require(cleanSubject.length in 3..160) { "Subject must be between 3 and 160 characters." }
        require(cleanDescription.length in 5..5000) { "Description must be between 5 and 5000 characters." }

        val token = AuthManager.validAccessToken(context)
        val userId = jwtSubject(token)
        require(userId.isNotBlank()) { "Could not identify the signed-in account." }
        val diagnostics = if (includeDiagnostics) JSONObject().apply {
            put("platform", "android")
            put("captured", System.currentTimeMillis())
            put("manufacturer", Build.MANUFACTURER)
            put("model", Build.MODEL)
            put("android", Build.VERSION.RELEASE)
        } else JSONObject()
        val payload = JSONObject().apply {
            put("user_id", userId)
            put("category", cleanCategory)
            put("subject", cleanSubject)
            put("description", cleanDescription)
            put("status", "open")
            put("priority", "normal")
            put("assigned_to", JSONObject.NULL)
            put("app_version", BuildConfig.VERSION_NAME)
            put("app_version_code", BuildConfig.VERSION_CODE)
            put("device_model", if (includeDiagnostics) "${Build.MANUFACTURER} ${Build.MODEL}".trim() else JSONObject.NULL)
            put("android_version", if (includeDiagnostics) Build.VERSION.RELEASE else JSONObject.NULL)
            put("diagnostics", diagnostics)
            put("diagnostics_opt_in", includeDiagnostics)
        }
        val (code, body) = request(
            path = "/rest/v1/support_tickets?select=id",
            method = "POST",
            token = token,
            contentType = "application/json",
            body = payload.toString().toByteArray(),
            prefer = "return=representation"
        )
        if (code !in 200..299) throw IllegalStateException(apiError(code, body, "Support ticket could not be submitted."))
        val ticketId = runCatching { JSONArray(body).getJSONObject(0).getString("id") }.getOrDefault("")
        require(ticketId.isNotBlank()) { "Ticket was created but its reference was not returned." }

        if (attachment == null) return SubmitResult(ticketId)
        val warning = runCatching { uploadAttachment(context, token, userId, ticketId, attachment) }
            .exceptionOrNull()?.message
        return SubmitResult(ticketId, warning)
    }

    private suspend fun uploadAttachment(context: Context, token: String, userId: String, ticketId: String, uri: Uri) {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri).orEmpty().lowercase()
        require(mime in allowedTypes) { "Attachment must be a JPG, PNG, WebP or PDF." }
        var name = "attachment"
        var size = -1L
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIndex >= 0) name = cursor.getString(nameIndex).orEmpty().ifBlank { "attachment" }
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex)
            }
        }
        if (size >= 0) require(size <= MAX_ATTACHMENT_BYTES) { "Attachment must be 10 MB or smaller." }
        val bytes = withContext(Dispatchers.IO) { resolver.openInputStream(uri)?.use { it.readBytes() } ?: error("Could not read attachment.") }
        require(bytes.size.toLong() <= MAX_ATTACHMENT_BYTES) { "Attachment must be 10 MB or smaller." }
        val safeName = name.replace(Regex("[^A-Za-z0-9._-]+"), "-").trim('-').take(120).ifBlank { "attachment" }
        val storagePath = "$userId/$ticketId/${System.currentTimeMillis()}-$safeName"
        val (uploadCode, uploadBody) = request(
            path = "/storage/v1/object/$ATTACHMENT_BUCKET/$storagePath",
            method = "POST",
            token = token,
            contentType = mime,
            body = bytes,
            extraHeaders = mapOf("x-upsert" to "false")
        )
        if (uploadCode !in 200..299) throw IllegalStateException(apiError(uploadCode, uploadBody, "Attachment upload failed."))
        val metadata = JSONObject().apply {
            put("ticket_id", ticketId)
            put("uploader_user_id", userId)
            put("storage_path", storagePath)
            put("file_name", name)
            put("content_type", mime)
            put("byte_size", bytes.size)
        }
        val (metaCode, metaBody) = request(
            path = "/rest/v1/support_ticket_attachments",
            method = "POST",
            token = token,
            contentType = "application/json",
            body = metadata.toString().toByteArray(),
            prefer = "return=minimal"
        )
        if (metaCode !in 200..299) throw IllegalStateException(apiError(metaCode, metaBody, "Attachment metadata could not be saved."))
    }

    private suspend fun request(
        path: String,
        method: String,
        token: String,
        contentType: String,
        body: ByteArray,
        prefer: String? = null,
        extraHeaders: Map<String, String> = emptyMap()
    ): Pair<Int, String> = withContext(Dispatchers.IO) {
        val connection = (URL("${BuildConfig.SUPABASE_URL}$path").openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 20_000
            doOutput = true
            setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Content-Type", contentType)
            if (!prefer.isNullOrBlank()) setRequestProperty("Prefer", prefer)
            extraHeaders.forEach { (key, value) -> setRequestProperty(key, value) }
        }
        try {
            connection.outputStream.use { it.write(body) }
            val responseCode = connection.responseCode
            val responseBody = (if (responseCode in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            responseCode to responseBody
        } finally {
            connection.disconnect()
        }
    }

    private fun jwtSubject(token: String): String = runCatching {
        val payload = token.split('.')[1].replace('-', '+').replace('_', '/')
        val padded = payload + "=".repeat((4 - payload.length % 4) % 4)
        JSONObject(String(android.util.Base64.decode(padded, android.util.Base64.DEFAULT))).optString("sub")
    }.getOrDefault("")

    private fun apiError(code: Int, body: String, fallback: String): String {
        val message = runCatching {
            val json = JSONObject(body)
            json.optString("message").ifBlank { json.optString("error") }.ifBlank { json.optString("hint") }
        }.getOrDefault("")
        return if (message.isNotBlank()) message else "$fallback ($code)"
    }
}
