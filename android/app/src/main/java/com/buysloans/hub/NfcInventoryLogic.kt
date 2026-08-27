package com.buysloans.hub

internal object NfcInventoryLogic {
    fun normalizeTagId(tagId: String): String = tagId.trim().uppercase()

    fun find(items: List<StockItem>, tagId: String): StockItem? {
        val clean = normalizeTagId(tagId)
        if (clean.isBlank()) return null
        return items.firstOrNull { it.nfcTagId.isNotBlank() && normalizeTagId(it.nfcTagId) == clean }
    }

    fun link(items: List<StockItem>, itemId: String, tagId: String): List<StockItem> {
        val clean = normalizeTagId(tagId)
        require(clean.isNotBlank()) { "Scan a valid NFC tag first." }
        require(items.any { it.id == itemId }) { "Inventory item no longer exists." }
        val conflict = items.firstOrNull { it.id != itemId && normalizeTagId(it.nfcTagId) == clean }
        require(conflict == null) { "This NFC tag is already linked to ${conflict?.name}." }
        return items.map { if (it.id == itemId) it.copy(nfcTagId = clean) else it }
    }

    fun unlink(items: List<StockItem>, itemId: String): List<StockItem> =
        items.map { if (it.id == itemId) it.copy(nfcTagId = "") else it }
}
