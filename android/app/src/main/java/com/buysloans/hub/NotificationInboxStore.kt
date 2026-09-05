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
        val read: Boolean,
        val versionCode: Int = 0,
        val versionName: String = "",
        val apkUrl: String = "",
        val notes: String = "",
        val sha256: String = ""
    )

    fun add(context: Context, title: String, body: String, type: String) {
        addItem(
            context,
            InboxItem(
                id = newId(title),
                title = title.trim().ifBlank { "B&L Morley" },
                body = body.trim(),
                type = type.ifBlank { "message" },
                createdAt = System.currentTimeMillis(),
                read = false
            )
        )
    }

    fun addUpdate(context: Context, update: AppUpdate) {
        addItem(
            context,
            InboxItem(
                id = newId("update-${update.versionCode}"),
                title = "B&L Morley update available",
                body = "Version ${update.versionName} is ready to download and install.",
                type = "update",
                createdAt = System.currentTimeMillis(),
                read = false,
                versionCode = update.versionCode,
                versionName = update.versionName,
                apkUrl = update.apkUrl,
                notes = update.notes,
                sha256 = update.sha256.lowercase()
            )
        )
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
                            read = item.optBoolean("read"),
                            versionCode = item.optInt("versionCode"),
                            versionName = item.optString("versionName"),
                            apkUrl = item.optString("apkUrl"),
                            notes = item.optString("notes"),
                            sha256 = item.optString("sha256").lowercase()
                        )
                    )
                }
            }
        }.getOrDefault(emptyList()).filterNot { item ->
            item.type.equals("update", ignoreCase = true) &&
                item.versionCode > 0 &&
                item.versionCode <= BuildConfig.VERSION_CODE
        }
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

    fun clearRead(context: Context) {
        save(context, items(context).filterNot { it.read })
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_ITEMS)
            .apply()
    }

    private fun addItem(context: Context, item: InboxItem) {
        val existing = items(context).toMutableList()
        existing.add(0, item)
        save(context, existing.take(MAX_ITEMS))
    }

    private fun newId(seed: String): String = "${System.currentTimeMillis()}-${seed.hashCode()}"

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
                    .put("versionCode", item.versionCode)
                    .put("versionName", item.versionName)
                    .put("apkUrl", item.apkUrl)
                    .put("notes", item.notes)
                    .put("sha256", item.sha256)
            )
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ITEMS, json.toString())
            .apply()
    }
}
