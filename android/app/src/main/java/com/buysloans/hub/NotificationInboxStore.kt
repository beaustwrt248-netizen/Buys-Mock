package com.buysloans.hub

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Small local notification history used by the in-app notification centre.
 * It deliberately stores only message metadata/content and never auth tokens.
 */
object NotificationInboxStore {
    private const val PREFS = "morley_notification_inbox"
    private const val KEY_ITEMS = "items"
    private const val MAX_ITEMS = 50

    data class InboxItem(
        val id: String,
        val title: String,
        val body: String,
        val type: String,
        val createdAt: Long,
        val read: Boolean
    )

    fun add(context: Context, title: String, body: String, type: String) {
        val existing = items(context).toMutableList()
        existing.add(
            0,
            InboxItem(
                id = "${System.currentTimeMillis()}-${title.hashCode()}",
                title = title.trim().ifBlank { "B&L Morley" },
                body = body.trim(),
                type = type.ifBlank { "message" },
                createdAt = System.currentTimeMillis(),
                read = false
            )
        )
        save(context, existing.take(MAX_ITEMS))
    }

    fun items(context: Context): List<InboxItem> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_ITEMS, "[]")
            .orEmpty()
        return runCatching {
            val json = JSONArray(raw)
            buildList {
                for (i in 0 until json.length()) {
                    val item = json.optJSONObject(i) ?: continue
                    add(
                        InboxItem(
                            id = item.optString("id"),
                            title = item.optString("title", "B&L Morley"),
                            body = item.optString("body"),
                            type = item.optString("type", "message"),
                            createdAt = item.optLong("createdAt"),
                            read = item.optBoolean("read")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun unreadCount(context: Context): Int = items(context).count { !it.read }

    fun markRead(context: Context, id: String) {
        if (id.isBlank()) return
        save(context, items(context).map { item ->
            if (item.id == id) item.copy(read = true) else item
        })
    }

    fun markAllRead(context: Context) {
        save(context, items(context).map { it.copy(read = true) })
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_ITEMS)
            .apply()
    }

    private fun save(context: Context, items: List<InboxItem>) {
        val json = JSONArray()
        items.take(MAX_ITEMS).forEach { item ->
            json.put(
                JSONObject()
                    .put("id", item.id)
                    .put("title", item.title)
                    .put("body", item.body)
                    .put("type", item.type)
                    .put("createdAt", item.createdAt)
                    .put("read", item.read)
            )
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ITEMS, json.toString())
            .apply()
    }
}
