package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MorleyVisionConditionFailClosedContractTest {
    private fun inspection(conditionGrade: String) = DeviceInspection(
        brand = "Apple",
        family = "iPhone",
        model = "iPhone 15",
        modelNumber = "A3090",
        colour = "Black",
        storage = "128 GB",
        conditionGrade = conditionGrade,
        conditionSummary = "",
        confidence = 0.95,
        damageFlags = emptyList(),
        damageRegions = emptyList(),
        evidence = emptyList(),
        uncertainties = emptyList(),
        nextPhotos = emptyList(),
        catalogueMatch = null
    )

    @Test
    fun unresolvedConditionBlocksSuggestedPricing() {
        val result = MorleyVisionPolicy.pricingBlockReason(inspection("UNVERIFIED"))

        assertEquals("Device condition is not verified strongly enough for a price recommendation.", result)
        assertTrue(!MorleyVisionPolicy.isConditionVerified(inspection("UNVERIFIED")))
    }

    @Test
    fun supportedConditionCanPassConditionGuard() {
        assertTrue(MorleyVisionPolicy.isConditionVerified(inspection("C")))
        assertNull(MorleyVisionPolicy.pricingBlockReason(inspection("C")))
    }

    @Test
    fun edgeFunctionReturnsExplicitUnverifiedCondition() {
        val edge = File("../../supabase/functions/device-inspection/index.ts").readText()

        assertTrue(edge.contains("condition_grade: normaliseGrade(parsed.condition_grade) || \"UNVERIFIED\""))
    }

    @Test
    fun androidClientNeverConvertsMissingConditionToFair() {
        val client = File("src/main/java/com/buysloans/hub/DeviceInspectionClient.kt").readText()

        assertTrue(client.contains("conditionGrade = MorleyVisionPolicy.clean(result.optString(\"condition_grade\")).ifBlank { \"UNVERIFIED\" }"))
        assertTrue(client.contains("else -> error(\"A verified condition grade is required before pricing.\")"))
    }
}
