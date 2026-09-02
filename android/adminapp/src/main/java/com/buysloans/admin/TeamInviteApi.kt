package com.buysloans.admin

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.SecureRandom
import java.time.Instant

internal data class TeamInvite(
    val id: String,
    val email: String,
    val displayName: String,
    val role: String,
    val expiresAt: String,
    val usedAt: String?,
    val createdAt: String
) {
    val isUsed: Boolean get() = !usedAt.isNullOrBlank()
    val isExpired: Boolean get() = runCatching { Instant.parse(expiresAt).isBefore(Instant.now()) }.getOrDefault(false)
}

internal data class TeamInviteSecret(val invite: TeamInvite, val code: String)
internal data class TemporaryUserSecret(
    val email: String,
    val displayName: String,
    val role: String,
    val temporaryPassword: String
)

internal fun temporaryUserPayload(displayName: String, email: String, role: String, temporaryPassword: String): String =
    JSONObject()
        .put("action", "create_user")
        .put("email", email.trim().lowercase())
        .put("display_name", displayName.trim().replace(Regex("\\s+"), " "))
        .put("role", role.trim().lowercase())
        .put("temporary_password", temporaryPassword)
        .put("skip_email_verification", true)
        .toString()

internal object TeamInviteApi {
    private const val PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#%+-_"

    suspend fun list(session: AdminSession): List<TeamInvite> = withContext(Dispatchers.IO) {
        require(TeamInvitePolicy.canManage(session)) { "Team invites require an Admin or Manager session." }
        val response = request(
            "/rest/v1/app_invites?select=id,email,display_name,role,expires_at,used_at,created_at&order=created_at.desc&limit=100",
            "GET",
            session.accessToken,
            null
        )
        if (response.first !in 200..299) error(message(response.second, "Team invites could not be loaded."))
        val data = JSONArray(response.second)
        (0 until data.length()).mapNotNull { index -> data.optJSONObject(index)?.let(::parse) }
    }

    suspend fun create(session: AdminSession, displayName: String, email: String, role: String): TeamInviteSecret =
        withContext(Dispatchers.IO) {
            TeamInvitePolicy.validate(session, displayName, email, role)
            val payload = JSONObject()
                .put("action", "create_invite")
                .put("email", email.trim().lowercase())
                .put("display_name", displayName.trim().replace(Regex("\\s+"), " "))
                .put("role", role.trim().lowercase())
                .toString()
            val response = request("/functions/v1/send-morley-email", "POST", session.accessToken, payload)
            if (response.first !in 200..299) error(message(response.second, "Team invite could not be created or emailed."))
            emailedInviteSecret(response.second, "Invite response was incomplete.")
        }

    suspend fun createTemporaryUser(
        session: AdminSession,
        displayName: String,
        email: String,
        role: String,
        temporaryPassword: String
    ): TemporaryUserSecret = withContext(Dispatchers.IO) {
        TeamInvitePolicy.validate(session, displayName, email, role)
        require(temporaryPassword.length in 10..256) { "Temporary password must be between 10 and 256 characters." }
        val cleanName = displayName.trim().replace(Regex("\\s+"), " ")
        val cleanEmail = email.trim().lowercase()
        val response = request(
            "/functions/v1/admin-user-control",
            "POST",
            session.accessToken,
            temporaryUserPayload(cleanName, cleanEmail, role, temporaryPassword)
        )
        if (response.first !in 200..299) error(message(response.second, "Temporary-password account could not be created."))
        val json = runCatching { JSONObject(response.second) }.getOrNull()
        require(json?.optBoolean("ok") == true && json.optBoolean("requires_password_change")) {
            "Temporary-password account creation was not confirmed."
        }
        TemporaryUserSecret(cleanEmail, cleanName, role, temporaryPassword)
    }

    fun generateTemporaryPassword(length: Int = 16): String {
        val safeLength = length.coerceIn(12, 32)
        val random = SecureRandom()
        return buildString {
            append('A' + random.nextInt(26))
            append('a' + random.nextInt(26))
            append('0' + random.nextInt(10))
            append("!@#%+-_"[random.nextInt(7)])
            repeat(safeLength - 4) { append(PASSWORD_CHARS[random.nextInt(PASSWORD_CHARS.length)]) }
        }.toCharArray().also { chars ->
            for (i in chars.indices.reversed()) {
                val j = random.nextInt(i + 1)
                val tmp = chars[i]; chars[i] = chars[j]; chars[j] = tmp
            }
        }.concatToString()
    }

    suspend fun reissue(session: AdminSession, invite: TeamInvite): TeamInviteSecret = withContext(Dispatchers.IO) {
        require(TeamInvitePolicy.canManage(session)) { "Team invites require an Admin or Manager session." }
        require(!invite.isUsed) { "Used invites cannot be reissued." }
        require(invite.role in TeamInvitePolicy.allowedRoles(session)) { "You are not allowed to reissue this role." }
        val payload = JSONObject()
            .put("action", "reissue_invite")
            .put("invite_id", invite.id)
            .toString()
        val response = request("/functions/v1/send-morley-email", "POST", session.accessToken, payload)
        if (response.first !in 200..299) error(message(response.second, "Invite could not be reissued or emailed."))
        emailedInviteSecret(response.second, "Invite is no longer active.")
    }

    suspend fun revoke(session: AdminSession, invite: TeamInvite) = withContext(Dispatchers.IO) {
        require(TeamInvitePolicy.canManage(session)) { "Team invites require an Admin or Manager session." }
        require(!invite.isUsed) { "Used invites cannot be revoked." }
        require(invite.role in TeamInvitePolicy.allowedRoles(session)) { "You are not allowed to revoke this role." }
        val payload = JSONObject().put("invite_id", invite.id).toString()
        val response = request("/rest/v1/rpc/admin_revoke_team_invite", "POST", session.accessToken, payload)
        if (response.first !in 200..299) error(message(response.second, "Invite could not be revoked."))
    }

    private fun emailedInviteSecret(body: String, fallback: String): TeamInviteSecret {
        val json = runCatching { JSONObject(body) }.getOrNull() ?: error(fallback)
        require(json.optBoolean("ok")) { message(body, fallback) }
        val invite = json.optJSONObject("invite")?.let(::parse) ?: error(fallback)
        val code = json.optString("invite_code").trim()
        require(code.isNotBlank()) { "Invite was emailed, but the one-time backup code was not returned." }
        return TeamInviteSecret(invite, code)
    }

    private fun parse(json: JSONObject) = TeamInvite(
        id = json.optString("id"),
        email = json.optString("email"),
        displayName = json.optString("display_name"),
        role = json.optString("role"),
        expiresAt = json.optString("expires_at"),
        usedAt = json.optString("used_at").takeIf { it.isNotBlank() && it != "null" },
        createdAt = json.optString("created_at")
    )

    private fun request(path: String, method: String, token: String, body: String?): Pair<Int, String> {
        val connection = (URL("${BuildConfig.SUPABASE_URL}$path").openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 20_000
            setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Accept", "application/json")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
        }
        return try {
            if (body != null) connection.outputStream.use { it.write(body.toByteArray()) }
            val code = connection.responseCode
            val text = (if (code in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
            code to text
        } finally {
            connection.disconnect()
        }
    }

    private fun message(body: String, fallback: String): String = runCatching {
        val json = JSONObject(body)
        json.optString("error").ifBlank { json.optString("message") }.ifBlank { json.optString("details") }.ifBlank { json.optString("hint") }.ifBlank { fallback }
    }.getOrDefault(fallback)
}
