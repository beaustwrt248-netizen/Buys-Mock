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

    data class SubmitResult(val ticketId: String, val attachmentWarning: String? = null)

    data class TicketSummary(
        val id: String,
        val category: String,
        val subject: String,
        val description: String,
        val status: String,
        val priority: String,
        val createdAt: String,
        val updatedAt: String
    )

    data class TicketMessage(
        val id: String,
        val ticketId: String,
        val authorRole: String,
        val body: String,
        val createdAt: String
    )

    suspend fun listMyTickets(context: Context): List<TicketSummary> {
        val token = AuthManager.validAccessToken(context)
        val (code, body) = request(
            path = "/rest/v1/support_tickets?select=id,category,subject,description,status,priority,created_at,updated_at&order=updated_at.desc",
            method = "GET",
            token = token
        )
        if (code !in 200..299) throw IllegalStateException(apiError(code, body, "Support tickets could not be loaded."))
        val json = JSONArray(body)
        return buildList {
            for (index in 0 until json.length()) {
                val item = json.getJSONObject(index)
                add(
                    TicketSummary(
                        id = item.optString("id"),
                        category = item.optString("category"),
                        subject = item.optString("subject"),
                        description = item.optString("description"),
                        status = item.optString("status"),
                        priority = item.optString("priority"),
                        createdAt = item.optString("created_at"),
                        updatedAt = item.optString("updated_at")
                    )
                )
            }
        }.filter { it.id.isNotBlank() }
    }

    suspend fun listMyMessages(context: Context): List<TicketMessage> {
        val token = AuthManager.validAccessToken(context)
        val (code, body) = request(
            path = "/rest/v1/support_ticket_messages?select=id,ticket_id,author_role,body,created_at&order=created_at.asc",
            method = "GET",
            token = token
        )
        if (code !in 200..299) throw IllegalStateException(apiError(code, body, "Support replies could not be loaded."))
        val json = JSONArray(body)
        return buildList {
            for (index in 0 until json.length()) {
                val item = json.getJSONObject(index)
                add(
                    TicketMessage(
                        id = item.optString("id"),
                        ticketId = item.optString("ticket_id"),
                        authorRole = item.optString("author_role"),
                        body = item.optString("body"),
                        createdAt = item.optString("created_at")
                    )
                )
            }
        }.filter { it.id.isNotBlank() && it.ticketId.isNotBlank() }
    }

    suspend fun reply(context: Context, ticketId: String, body: String) {
        val cleanTicketId = ticketId.trim()
        require(cleanTicketId.isNotBlank()) { "Choose a support ticket first." }
        val cleanBody = SupportTicketLogic.validateReply(body)
        val token = AuthManager.validAccessToken(context)
        val payload = JSONObject().apply {
            put("ticket_id", cleanTicketId)
            put("author_role", "user")
            put("body", cleanBody)
        }
        val (code, response) = request(
            path = "/rest/v1/support_ticket_messages",
            method = "POST",
            token = token,
            contentType = "application/json",
            body = payload.toString().toByteArray(),
            prefer = "return=minimal"
        )
        if (code !in 200..299) throw IllegalStateException(apiError(code, response, "Reply could not be sent."))
    }

    suspend fun submit(
        context: Context,
        category: String,
        subject: String,
        description: String,
        includeDiagnostics: Boolean,
        attachment: Uri? = null
    ): SubmitResult {
        val draft = SupportTicketLogic.validateDraft(category, subject, description)

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
            put("category", draft.category)
            put("subject", draft.subject)
            put("description", draft.description)
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
        SupportTicketLogic.validateAttachment(mime, size.takeIf { it >= 0 })
        val bytes = withContext(Dispatchers.IO) { resolver.openInputStream(uri)?.use { it.readBytes() } ?: error("Could not read attachment.") }
        SupportTicketLogic.validateAttachment(mime, bytes.size.toLong())
        val safeName = SupportTicketLogic.safeFileName(name)
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
        contentType: String = "application/json",
        body: ByteArray? = null,
        prefer: String? = null,
        extraHeaders: Map<String, String> = emptyMap()
    ): Pair<Int, String> = withContext(Dispatchers.IO) {
        val connection = (URL("${BuildConfig.SUPABASE_URL}$path").openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 20_000
            doOutput = body != null
            setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Accept", "application/json")
            if (body != null) setRequestProperty("Content-Type", contentType)
            if (!prefer.isNullOrBlank()) setRequestProperty("Prefer", prefer)
            extraHeaders.forEach { (key, value) -> setRequestProperty(key, value) }
        }
        try {
            if (body != null) connection.outputStream.use { it.write(body) }
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
