package com.buysloans.admin

import org.json.JSONArray
import org.json.JSONObject

internal const val SUPPORT_QUEUE_VISIBLE_LIMIT = 50
internal const val SUPPORT_ASSIGNEE_UNASSIGNED = "__unassigned__"

internal data class SupportQueueFilter(
    val query: String = "",
    val status: String = "",
    val priority: String = "",
    val assignee: String = ""
)

internal fun filterSupportQueue(
    tickets: JSONArray?,
    filter: SupportQueueFilter
): List<JSONObject> {
    if (tickets == null) return emptyList()
    val query = filter.query.trim().lowercase()
    val limit = minOf(tickets.length(), SUPPORT_QUEUE_VISIBLE_LIMIT)
    val result = ArrayList<JSONObject>(limit)

    for (index in 0 until limit) {
        val ticket = tickets.optJSONObject(index) ?: continue
        if (filter.status.isNotBlank() && ticket.optString("status") != filter.status) continue
        if (filter.priority.isNotBlank() && ticket.optString("priority") != filter.priority) continue

        val assignedTo = ticket.optString("assigned_to").takeUnless { it == "null" }.orEmpty()
        when (filter.assignee) {
            "" -> Unit
            SUPPORT_ASSIGNEE_UNASSIGNED -> if (assignedTo.isNotBlank()) continue
            else -> if (assignedTo != filter.assignee) continue
        }

        if (query.isNotBlank() && !ticketSearchText(ticket).contains(query)) continue
        result += ticket
    }
    return result
}

private fun ticketSearchText(ticket: JSONObject): String = buildString {
    listOf(
        "id",
        "subject",
        "description",
        "category",
        "status",
        "priority",
        "assigned_to",
        "app_version",
        "device_model",
        "android_version"
    ).forEach { key ->
        val value = ticket.optString(key)
        if (value.isNotBlank() && value != "null") {
            append(value.lowercase())
            append('\n')
        }
    }
}
