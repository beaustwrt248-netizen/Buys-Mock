package com.buysloans.hub

enum class DeviceTestHistoryFilter {
    ALL,
    BARCODE,
    TEST_BUY,
    ANDROID_NFC_READ_ONLY
}

data class DeviceTestHistoryWorkspaceSnapshot(
    val filter: DeviceTestHistoryFilter,
    val totalCount: Int,
    val visibleCount: Int,
    val barcodeCount: Int,
    val testBuyCount: Int,
    val androidNfcReadOnlyCount: Int,
    val items: List<DeviceTestHistoryTimelineItem>
)

/**
 * Read-only presentation boundary for the device-testing scan/test history workspace.
 *
 * This model never performs inventory lookup, assignment, linking/unlinking or stock mutation.
 * Android NFC remains a read-only history source and is intentionally named as such in both
 * filtering and timeline presentation.
 */
object DeviceTestHistoryWorkspace {
    fun snapshot(
        entries: List<DeviceTestHistoryEntry>,
        filter: DeviceTestHistoryFilter = DeviceTestHistoryFilter.ALL,
        limit: Int = 25
    ): DeviceTestHistoryWorkspaceSnapshot {
        val source = when (filter) {
            DeviceTestHistoryFilter.ALL -> null
            DeviceTestHistoryFilter.BARCODE -> DeviceTestHistorySource.BARCODE
            DeviceTestHistoryFilter.TEST_BUY -> DeviceTestHistorySource.TEST_BUY
            DeviceTestHistoryFilter.ANDROID_NFC_READ_ONLY -> DeviceTestHistorySource.NFC
        }
        val visible = DeviceTestHistoryQuery.recent(entries, source = source, limit = limit)
        return DeviceTestHistoryWorkspaceSnapshot(
            filter = filter,
            totalCount = entries.size,
            visibleCount = visible.size,
            barcodeCount = entries.count { it.source == DeviceTestHistorySource.BARCODE },
            testBuyCount = entries.count { it.source == DeviceTestHistorySource.TEST_BUY },
            androidNfcReadOnlyCount = entries.count { it.source == DeviceTestHistorySource.NFC },
            items = visible.map(DeviceTestHistoryTimeline::toTimelineItem)
        )
    }
}
