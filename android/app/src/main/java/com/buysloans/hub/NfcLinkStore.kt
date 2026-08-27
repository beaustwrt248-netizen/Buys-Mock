package com.buysloans.hub

import android.content.Context

object NfcLinkStore {
    private const val PREFS = "morley_nfc_links"

    fun linkedStockId(context: Context, tagId: String): String? = context
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getString(tagId.uppercase(), null)

    fun linkedStock(context: Context, tagId: String): StockItem? {
        val stockId = linkedStockId(context, tagId) ?: return null
        return WorkspaceStore.inventory(context).firstOrNull { it.id == stockId }
    }

    fun link(context: Context, tagId: String, stockId: String) {
        require(tagId.isNotBlank()) { "Scan an NFC tag first." }
        require(WorkspaceStore.inventory(context).any { it.id == stockId }) { "That inventory item no longer exists." }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(tagId.uppercase(), stockId)
            .apply()
    }

    fun unlink(context: Context, tagId: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(tagId.uppercase())
            .apply()
    }
}
