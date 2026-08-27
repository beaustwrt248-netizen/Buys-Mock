package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class NfcInventoryLogicTest {
    private fun item(id: String, name: String, tag: String = "") = StockItem(
        id = id,
        name = name,
        barcode = "",
        cost = 10.0,
        resale = 20.0,
        quantity = 1,
        createdAt = 1L,
        nfcTagId = tag
    )

    @Test
    fun findsInventoryByNormalizedTagId() {
        val items = listOf(item("1", "Console", "A1B2"), item("2", "Game"))
        assertEquals("Console", NfcInventoryLogic.find(items, " a1b2 ")?.name)
        assertNull(NfcInventoryLogic.find(items, "FFFF"))
    }

    @Test
    fun linksScannedTagToSelectedInventoryItem() {
        val linked = NfcInventoryLogic.link(listOf(item("1", "Console"), item("2", "Game")), "2", " a1b2 ")
        assertEquals("A1B2", linked.first { it.id == "2" }.nfcTagId)
        assertEquals("", linked.first { it.id == "1" }.nfcTagId)
    }

    @Test
    fun rejectsDuplicateTagAssignmentToDifferentStockItem() {
        val items = listOf(item("1", "Console", "A1B2"), item("2", "Game"))
        assertThrows(IllegalArgumentException::class.java) {
            NfcInventoryLogic.link(items, "2", "a1b2")
        }
    }

    @Test
    fun unlinksTagWithoutChangingOtherInventory() {
        val items = listOf(item("1", "Console", "A1B2"), item("2", "Game", "C3D4"))
        val result = NfcInventoryLogic.unlink(items, "1")
        assertEquals("", result.first { it.id == "1" }.nfcTagId)
        assertEquals("C3D4", result.first { it.id == "2" }.nfcTagId)
    }
}
