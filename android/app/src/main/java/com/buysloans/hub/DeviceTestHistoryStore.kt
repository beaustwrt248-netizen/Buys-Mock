package com.buysloans.hub

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class DeviceTestHistorySource { BARCODE, NFC, TEST_BUY }

data class DeviceTestHistoryEntry(
    val id:String,
    val source:DeviceTestHistorySource,
    val reference:String,
    val itemName:String,
    val category:String,
    val result:String,
    val summary:String,
    val recordedAt:Long
)

object DeviceTestHistoryStore {
    private const val PREFS = "morley_device_test_history"
    private const val HISTORY = "entries_json"
    private const val MAX_ENTRIES = 100

    private fun prefs(context:Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun entries(context:Context):List<DeviceTestHistoryEntry> = decode(
        prefs(context).getString(HISTORY, "[]") ?: "[]"
    )

    fun record(
        context:Context,
        source:DeviceTestHistorySource,
        reference:String,
        itemName:String = "",
        category:String = "",
        result:String,
        summary:String,
        recordedAt:Long = System.currentTimeMillis()
    ):DeviceTestHistoryEntry {
        require(reference.isNotBlank() || itemName.isNotBlank()) { "History entry needs an item or scan reference." }
        val entry = DeviceTestHistoryEntry(
            id = UUID.randomUUID().toString(),
            source = source,
            reference = reference.trim(),
            itemName = itemName.trim(),
            category = category.trim(),
            result = result.trim(),
            summary = summary.trim(),
            recordedAt = recordedAt
        )
        val updated = (listOf(entry) + entries(context)).take(MAX_ENTRIES)
        prefs(context).edit().putString(HISTORY, encode(updated)).apply()
        return entry
    }

    fun clear(context:Context) {
        prefs(context).edit().remove(HISTORY).apply()
    }

    internal fun encode(entries:List<DeviceTestHistoryEntry>):String {
        val array = JSONArray()
        entries.forEach { entry ->
            array.put(JSONObject().apply {
                put("id", entry.id)
                put("source", entry.source.name)
                put("reference", entry.reference)
                put("itemName", entry.itemName)
                put("category", entry.category)
                put("result", entry.result)
                put("summary", entry.summary)
                put("recordedAt", entry.recordedAt)
            })
        }
        return array.toString()
    }

    internal fun decode(text:String):List<DeviceTestHistoryEntry> {
        val array = runCatching { JSONArray(text) }.getOrElse { JSONArray() }
        return (0 until array.length()).mapNotNull { index ->
            val json = array.optJSONObject(index) ?: return@mapNotNull null
            val source = runCatching { DeviceTestHistorySource.valueOf(json.optString("source")) }.getOrNull()
                ?: return@mapNotNull null
            DeviceTestHistoryEntry(
                id = json.optString("id"),
                source = source,
                reference = json.optString("reference"),
                itemName = json.optString("itemName"),
                category = json.optString("category"),
                result = json.optString("result"),
                summary = json.optString("summary"),
                recordedAt = json.optLong("recordedAt")
            )
        }
    }
}
