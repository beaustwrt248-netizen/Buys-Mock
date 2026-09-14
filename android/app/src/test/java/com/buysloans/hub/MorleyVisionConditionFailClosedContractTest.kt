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
        val item = inspection("UNVERIFIED")
        val result = MorleyVisionPolicy.pricingBlockReason(item)

        assertEquals("Staff must confirm the device condition before using a price recommendation.", result)
        assertTrue(!MorleyVisionPolicy.isConditionVerified(item))
    }

    @Test
    fun supportedAiConditionStillRequiresStaffConfirmationForPricing() {
        val item = inspection("C")

        assertTrue(MorleyVisionPolicy.isConditionVerified(item))
        assertEquals(
            "Staff must confirm the device condition before using a price recommendation.",
            MorleyVisionPolicy.pricingBlockReason(item)
        )

        item.staffConfirmedConditionGrade = "C"
        assertNull(MorleyVisionPolicy.pricingBlockReason(item))
        assertEquals("C", MorleyVisionPolicy.pricingConditionGrade(item))
    }

    @Test
    fun staffCanResolveUnverifiedAiConditionWithoutOverwritingAiEvidence() {
        val item = inspection("UNVERIFIED")
        item.staffConfirmedConditionGrade = "D"

        assertEquals("UNVERIFIED", item.conditionGrade)
        assertEquals("D", MorleyVisionPolicy.pricingConditionGrade(item))
        assertNull(MorleyVisionPolicy.pricingBlockReason(item))
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
