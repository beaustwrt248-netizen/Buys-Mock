package com.buysloans.hub

data class DeviceTestHistoryTimelineItem(
    val sourceLabel: String,
    val evidenceLabel: String,
    val itemLabel: String,
    val result: String,
    val summary: String,
    val recordedAt: Long,
    val isAndroidNfcReadOnly: Boolean
)

object DeviceTestHistoryTimeline {
    fun from(entries: List<DeviceTestHistoryEntry>, limit: Int = 25): List<DeviceTestHistoryTimelineItem> =
        DeviceTestHistoryQuery.recent(entries, limit = limit).map(::toTimelineItem)

    fun toTimelineItem(entry: DeviceTestHistoryEntry): DeviceTestHistoryTimelineItem {
        val sourceLabel = when (entry.source) {
            DeviceTestHistorySource.BARCODE -> "Barcode"
            DeviceTestHistorySource.TEST_BUY -> "Test & Buy"
            DeviceTestHistorySource.NFC -> "Android NFC read-only"
        }
        val evidenceLabel = entry.reference.ifBlank { "Manual entry" }
        val itemLabel = entry.itemName.ifBlank { "Unlabelled scan" }
        return DeviceTestHistoryTimelineItem(
            sourceLabel = sourceLabel,
            evidenceLabel = evidenceLabel,
            itemLabel = itemLabel,
            result = entry.result,
            summary = entry.summary,
            recordedAt = entry.recordedAt,
            isAndroidNfcReadOnly = entry.source == DeviceTestHistorySource.NFC
        )
    }
}
