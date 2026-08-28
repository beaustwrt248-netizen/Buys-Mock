package com.buysloans.admin

import org.json.JSONArray

internal data class SupportMessageViewItem(
    val authorRole: String,
    val body: String,
    val createdAt: String
)

internal fun supportMessageViewItems(messages: JSONArray?): List<SupportMessageViewItem> {
    if (messages == null) return emptyList()
    return buildList {
        for (i in 0 until minOf(messages.length(), 100)) {
            val row = messages.optJSONObject(i) ?: continue
            val body = row.optString("body").trim()
            if (body.isBlank()) continue
            add(
                SupportMessageViewItem(
                    authorRole = row.optString("author_role").ifBlank { "user" }.lowercase(),
                    body = body,
                    createdAt = row.optString("created_at")
                )
            )
        }
    }
}
