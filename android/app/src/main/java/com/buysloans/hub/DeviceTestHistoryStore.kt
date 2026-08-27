package com.buysloans.hub

import android.content.Context
import java.nio.charset.StandardCharsets
import java.util.Base64
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
    private const val FIELD_COUNT = 8

    private fun prefs(context:Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun entries(context:Context):List<DeviceTestHistoryEntry> = decode(
        prefs(context).getString(HISTORY, "") ?: ""
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

    private fun pack(value:String):String = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    private fun unpack(value:String):String = String(
        Base64.getUrlDecoder().decode(value),
        StandardCharsets.UTF_8
    )

    internal fun encode(entries:List<DeviceTestHistoryEntry>):String = entries
        .take(MAX_ENTRIES)
        .joinToString("\n") { entry ->
            listOf(
                pack(entry.id),
                entry.source.name,
                pack(entry.reference),
                pack(entry.itemName),
                pack(entry.category),
                pack(entry.result),
                pack(entry.summary),
                entry.recordedAt.toString()
            ).joinToString("|")
        }

    internal fun decode(text:String):List<DeviceTestHistoryEntry> {
        if (text.isBlank()) return emptyList()
        return text.lineSequence().take(MAX_ENTRIES).mapNotNull { line ->
            runCatching {
                val fields = line.split('|')
                if (fields.size != FIELD_COUNT) return@runCatching null
                val source = DeviceTestHistorySource.valueOf(fields[1])
                val recordedAt = fields[7].toLong()
                DeviceTestHistoryEntry(
                    id = unpack(fields[0]),
                    source = source,
                    reference = unpack(fields[2]),
                    itemName = unpack(fields[3]),
                    category = unpack(fields[4]),
                    result = unpack(fields[5]),
                    summary = unpack(fields[6]),
                    recordedAt = recordedAt
                )
            }.getOrNull()
        }.filterNotNull().toList()
    }
}
