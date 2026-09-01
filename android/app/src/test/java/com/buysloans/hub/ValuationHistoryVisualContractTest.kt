package com.buysloans.hub

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ValuationHistoryVisualContractTest {
    private fun sourceFile(name: String): String {
        val roots = listOf(File("src/main/java"), File("app/src/main/java"), File("android/app/src/main/java"))
        val root = roots.firstOrNull { it.exists() }
        assertNotNull("Could not locate Morley Android source", root)
        val file = root!!.walkTopDown().firstOrNull { it.isFile && it.name == name }
        assertNotNull("Could not locate $name", file)
        return file!!.readText()
    }

    @Test fun valuationHistoryOwnsItsLightBackgroundAndReadableFilters() {
        val history = sourceFile("ValuationHistoryActivity.kt")
        assertTrue("Valuations screen must paint the light Morley background", history.contains("fillMaxSize().background(HistBg)"))
        assertTrue("Status filters must divide available phone width evenly", history.contains("modifier=Modifier.weight(1f)"))
        assertTrue("Status filter labels must remain single-line", history.contains("maxLines=1,softWrap=false"))
        assertTrue("Selected filter must use pale mint with dark emerald text", history.contains("selectedContainerColor=Color(0xFFDDF4E9),selectedLabelColor=HistStrong"))
        assertTrue("Empty state must use a white card", history.contains("containerColor=Color.White"))
        assertTrue("Empty state guidance must be present", history.contains("Get started by saving your first valuation."))
    }

    @Test fun valuationHistorySellerAskCopyRemainsCanonical() {
        val history = sourceFile("ValuationHistoryActivity.kt")
        assertFalse("Legacy Seller asking price wording returned", history.contains("Seller asking price"))
        assertTrue("Canonical Seller Ask label is missing", history.contains("label={Text(\"Seller Ask\")}"))
    }
}