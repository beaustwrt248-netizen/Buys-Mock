package com.buysloans.hub

import android.accounts.Account
import android.content.Context
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

object DriveBackupClient {
    const val DRIVE_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
    private const val EDGE_PATH = "/functions/v1/user-google-drive-backup"

    data class BackupRow(
        val id: String,
        val createdAt: String,
        val byteSize: Long,
        val status: String
    )

    data class Snapshot(
        val googleEmail: String,
        val lastBackupAt: String,
        val backups: List<BackupRow>
    )

    suspend fun googleAccessToken(context: Context, signedIn: GoogleSignInAccount): String = withContext(Dispatchers.IO) {
        val account: Account = signedIn.account ?: error("Google account is unavailable. Connect Google Drive again.")
        GoogleAuthUtil.getToken(context, account, "oauth2:$DRIVE_SCOPE").also {
            require(it.isNotBlank()) { "Google Drive authorization did not return an access token." }
        }
    }

    private fun nativeClientState(context: Context): JSONObject {
        val workspace = JSONObject(WorkspaceStore.exportJson(context))
        return JSONObject()
            .put("bm_inv", workspace.optJSONArray("inventory") ?: JSONArray())
            .put("bm_sales", workspace.optJSONArray("sales") ?: JSONArray())
            .put("bm_recent", JSONArray())
    }

    private fun restoreNativeClientState(context: Context, state: JSONObject?) {
        if (state == null) return
        val inventory = state.optJSONArray("bm_inv") ?: JSONArray()
        val sales = state.optJSONArray("bm_sales") ?: JSONArray()
        val native = JSONObject()
            .put("format", "bl-morley-workspace-v1")
            .put("exportedAt", System.currentTimeMillis())
            .put("inventory", inventory)
            .put("sales", sales)
        WorkspaceStore.importJson(context, native.toString())
    }

    private suspend fun request(
        context: Context,
        googleToken: String,
        action: String,
        extra: JSONObject = JSONObject()
    ): JSONObject {
        val morleyToken = AuthManager.validAccessToken(context)
        return withContext(Dispatchers.IO) {
            val payload = JSONObject().put("action", action)
            val keys = extra.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                payload.put(key, extra.get(key))
            }
            val connection = (URL(BuildConfig.SUPABASE_URL + EDGE_PATH).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 30_000
                doOutput = true
                useCaches = false
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Authorization", "Bearer $morleyToken")
                setRequestProperty("X-Google-Access-Token", googleToken)
            }
            try {
                connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                val body = runCatching { JSONObject(text) }.getOrElse { JSONObject() }
                if (code !in 200..299) {
                    val detail = body.optString("error").ifBlank { body.optString("message") }.ifBlank { "Backup request failed ($code)." }
                    throw IllegalStateException(detail)
                }
                body
            } finally {
                connection.disconnect()
            }
        }
    }

    private fun localTimestamp(value: String): String {
        if (value.isBlank()) return value
        val normalized = if (value.length > 10 && value[10] == ' ') {
            value.substring(0, 10) + "T" + value.substring(11)
        } else value
        return runCatching {
            val formatter = DateTimeFormatter
                .ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                .withLocale(Locale.getDefault())
            OffsetDateTime.parse(normalized)
                .atZoneSameInstant(ZoneId.systemDefault())
                .format(formatter)
        }.getOrElse { value }
    }

    suspend fun createBackup(context: Context, googleToken: String, reason: String = "android-manual"): JSONObject =
        request(context, googleToken, "backup", JSONObject().put("reason", reason).put("client_state", nativeClientState(context)))

    suspend fun snapshot(context: Context, googleToken: String): Snapshot {
        val list = request(context, googleToken, "list")
        val health = request(context, googleToken, "health")
        val google = list.optJSONObject("google") ?: health.optJSONObject("google")
        val googleEmail = google?.optString("email").orEmpty()
        val last = localTimestamp(health.optJSONObject("last_backup")?.optString("created_at").orEmpty())
        val rows = mutableListOf<BackupRow>()
        val arr = list.optJSONArray("backups") ?: JSONArray()
        for (i in 0 until arr.length()) {
            val row = arr.optJSONObject(i) ?: continue
            val id = row.optString("id")
            if (id.isBlank()) continue
            rows += BackupRow(
                id = id,
                createdAt = localTimestamp(row.optString("created_at")),
                byteSize = row.optLong("byte_size"),
                status = row.optString("status", "ready")
            )
        }
        return Snapshot(googleEmail, last, rows)
    }

    suspend fun previewRestore(context: Context, googleToken: String, backupId: String): JSONObject =
        request(context, googleToken, "preview_restore", JSONObject().put("backup_id", backupId)).optJSONObject("preview") ?: JSONObject()

    suspend fun restore(context: Context, googleToken: String, backupId: String): JSONObject {
        val response = request(
            context,
            googleToken,
            "restore",
            JSONObject()
                .put("backup_id", backupId)
                .put("confirm", "RESTORE")
                .put("current_client_state", nativeClientState(context))
        )
        val restore = response.optJSONObject("restore") ?: response
        restoreNativeClientState(context, restore.optJSONObject("client_state"))
        return response
    }

    suspend fun verify(context: Context, googleToken: String, backupId: String): JSONObject =
        request(context, googleToken, "verify", JSONObject().put("backup_id", backupId))

    suspend fun delete(context: Context, googleToken: String, backupId: String): JSONObject =
        request(context, googleToken, "delete", JSONObject().put("backup_id", backupId))
}
