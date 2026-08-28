package com.buysloans.hub

object DeviceTestHistoryQuery {
    private const val DEFAULT_LIMIT = 25
    private const val MAX_LIMIT = 100

    fun recent(
        entries: List<DeviceTestHistoryEntry>,
        source: DeviceTestHistorySource? = null,
        reference: String = "",
        itemName: String = "",
        limit: Int = DEFAULT_LIMIT
    ): List<DeviceTestHistoryEntry> {
        val cleanReference = reference.trim()
        val cleanItemName = itemName.trim()
        val safeLimit = limit.coerceIn(1, MAX_LIMIT)

        return entries.asSequence()
            .filter { source == null || it.source == source }
            .filter { cleanReference.isBlank() || it.reference.equals(cleanReference, ignoreCase = true) }
            .filter { cleanItemName.isBlank() || it.itemName.equals(cleanItemName, ignoreCase = true) }
            .sortedWith(compareByDescending<DeviceTestHistoryEntry> { it.recordedAt }.thenBy { it.id })
            .take(safeLimit)
            .toList()
    }

    fun latestForEvidence(
        entries: List<DeviceTestHistoryEntry>,
        source: DeviceTestHistorySource,
        reference: String
    ): DeviceTestHistoryEntry? {
        val cleanReference = reference.trim()
        require(cleanReference.isNotBlank()) { "Evidence reference is required." }
        return recent(entries, source = source, reference = cleanReference, limit = 1).firstOrNull()
    }
}
