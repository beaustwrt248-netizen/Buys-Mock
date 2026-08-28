package com.buysloans.admin

import org.json.JSONArray

internal data class ProtectedSupportMessage(
    val roleLabel: String,
    val body: String,
    val createdAt: String
)

internal object SupportMessagePresentation {
    private const val MaxDisplayedBodyChars = 4000
    private const val MaxMessages = 100
    private val visibleRoles = setOf("admin", "manager", "staff", "user")

    fun present(messages: JSONArray?): List<ProtectedSupportMessage> {
        if (messages == null) return emptyList()
        val count = minOf(messages.length(), MaxMessages)
        return buildList {
            for (i in 0 until count) {
                val row = messages.optJSONObject(i) ?: continue
                val role = row.optString("author_role").lowercase().takeIf { it in visibleRoles } ?: "participant"
                val body = row.optString("body").trim().take(MaxDisplayedBodyChars)
                if (body.isBlank()) continue
                add(
                    ProtectedSupportMessage(
                        roleLabel = role.replaceFirstChar { it.uppercase() },
                        body = body,
                        createdAt = row.optString("created_at")
                    )
                )
            }
        }
    }
}
