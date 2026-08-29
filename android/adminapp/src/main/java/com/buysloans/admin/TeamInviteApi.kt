package com.buysloans.admin

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.time.temporal.ChronoUnit

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

internal object TeamInviteApi {
    private const val CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

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
        (0 until data.length()).mapNotNull { index ->
            data.optJSONObject(index)?.let(::parse)
        }
    }

    suspend fun create(session: AdminSession, displayName: String, email: String, role: String): TeamInviteSecret =
        withContext(Dispatchers.IO) {
            TeamInvitePolicy.validate(session, displayName, email, role)
            val code = makeCode()
            val hash = sha256(code)
            val expiresAt = Instant.now().plus(7, ChronoUnit.DAYS).toString()
            val payload = teamInvitePayload(session, displayName, email, role, hash, expiresAt)
            val response = request("/rest/v1/app_invites?select=id,email,display_name,role,expires_at,used_at,created_at", "POST", session.accessToken, payload, preferRepresentation = true)
            if (response.first !in 200..299) error(message(response.second, "Team invite could not be created."))
            val invite = JSONArray(response.second).optJSONObject(0)?.let(::parse) ?: error("Invite response was incomplete.")
            TeamInviteSecret(invite, code)
        }

    suspend fun reissue(session: AdminSession, invite: TeamInvite): TeamInviteSecret = withContext(Dispatchers.IO) {
        require(TeamInvitePolicy.canManage(session)) { "Team invites require an Admin or Manager session." }
        require(!invite.isUsed) { "Used invites cannot be reissued." }
        require(invite.role in TeamInvitePolicy.allowedRoles(session)) { "You are not allowed to reissue this role." }
        val code = makeCode()
        val expiresAt = Instant.now().plus(7, ChronoUnit.DAYS).toString()
        val payload = JSONObject().put("code_hash", sha256(code)).put("expires_at", expiresAt).toString()
        val response = request(
            "/rest/v1/app_invites?id=eq.${enc(invite.id)}&used_at=is.null&select=id,email,display_name,role,expires_at,used_at,created_at",
            "PATCH",
            session.accessToken,
            payload,
            preferRepresentation = true
        )
        if (response.first !in 200..299) error(message(response.second, "Invite could not be reissued."))
        val updated = JSONArray(response.second).optJSONObject(0)?.let(::parse) ?: error("Invite is no longer active.")
        TeamInviteSecret(updated, code)
    }

    suspend fun revoke(session: AdminSession, invite: TeamInvite) = withContext(Dispatchers.IO) {
        require(TeamInvitePolicy.canManage(session)) { "Team invites require an Admin or Manager session." }
        require(!invite.isUsed) { "Used invites cannot be revoked." }
        require(invite.role in TeamInvitePolicy.allowedRoles(session)) { "You are not allowed to revoke this role." }
        val response = request("/rest/v1/app_invites?id=eq.${enc(invite.id)}&used_at=is.null", "DELETE", session.accessToken, null)
        if (response.first !in 200..299) error(message(response.second, "Invite could not be revoked."))
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

    private fun makeCode(): String {
        val random = SecureRandom()
        return "BLM-" + List(3) {
            buildString {
                repeat(4) { append(CODE_CHARS[random.nextInt(CODE_CHARS.length)]) }
            }
        }.joinToString("-")
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.trim().toByteArray())
        .joinToString("") { "%02x".format(it) }

    private fun request(path: String, method: String, token: String, body: String?, preferRepresentation: Boolean = false): Pair<Int, String> {
        val connection = (URL("${BuildConfig.SUPABASE_URL}$path").openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 20_000
            setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Accept", "application/json")
            if (preferRepresentation) setRequestProperty("Prefer", "return=representation") else if (method != "GET") setRequestProperty("Prefer", "return=minimal")
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
        json.optString("message").ifBlank { json.optString("details") }.ifBlank { json.optString("hint") }.ifBlank { fallback }
    }.getOrDefault(fallback)

    private fun enc(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())
}
