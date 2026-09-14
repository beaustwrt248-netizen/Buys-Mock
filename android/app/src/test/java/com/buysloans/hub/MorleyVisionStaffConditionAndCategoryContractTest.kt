package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MorleyVisionStaffConditionAndCategoryContractTest {
    private fun inspection(conditionGrade: String = "B") = DeviceInspection(
        brand = "Apple",
        family = "iPhone",
        model = "iPhone 15",
        modelNumber = "A3090",
        colour = "Black",
        storage = "256GB",
        conditionGrade = conditionGrade,
        conditionSummary = "Good used condition",
        confidence = 0.95,
        damageFlags = emptyList(),
        damageRegions = emptyList(),
        evidence = emptyList(),
        uncertainties = emptyList(),
        nextPhotos = emptyList(),
        catalogueMatch = CatalogueMatch(
            id = "iphone-15",
            category = "phones",
            brand = "Apple",
            family = "iPhone",
            modelName = "iPhone 15",
            modelNumber = "A3090",
            releaseYear = 2023,
            storageOptions = listOf("128GB", "256GB"),
            imageReferenceUrl = null
        )
    )

    @Test
    fun staffMustExplicitlyConfirmConditionBeforeReviewCompletes() {
        val state = MorleyVisionReviewPolicy.from(inspection(), null)

        assertFalse(state.conditionVerifiedByStaff)
        assertFalse(state.canCompleteStaffReview)

        assertTrue(MorleyVisionReviewPolicy.confirmCondition(state, "Good"))
        assertTrue(state.conditionVerifiedByStaff)
        assertEquals("B", state.staffConditionGrade)
        assertEquals("B", state.inspection.conditionGrade)
        assertTrue(state.canCompleteStaffReview)
    }

    @Test
    fun unverifiedAiConditionCanBeResolvedByStaffWithoutChangingAiEvidence() {
        val state = MorleyVisionReviewPolicy.from(inspection("UNVERIFIED"), null)

        assertEquals("UNVERIFIED", state.inspection.conditionGrade)
        assertTrue(MorleyVisionReviewPolicy.confirmCondition(state, "Poor"))
        assertEquals("D", state.staffConditionGrade)
        assertEquals("UNVERIFIED", state.inspection.conditionGrade)
        assertEquals("D", MorleyVisionReviewPolicy.pricingConditionGrade(state))
    }

    @Test
    fun genericScannerNeverDefaultsUnknownCategoryToPhoneGuidance() {
        val edge = File("../../supabase/functions/device-inspection/index.ts").readText()

        assertTrue(edge.contains("categoryGuidance(clean(body?.category_hint, 40) || \"generic\")"))
        assertFalse(edge.contains("categoryGuidance(clean(body?.category_hint, 40) || \"phone\")"))
        assertTrue(edge.contains("case \"generic\""))
    }

    @Test
    fun stockEntryMustUseStaffConfirmedConditionRatherThanAiSuggestion() {
        val activity = File("src/main/java/com/buysloans/hub/DeviceLensActivity.kt").readText()

        assertFalse(activity.contains("confirmedCondition = gradeLabel(it.conditionGrade)"))
        assertFalse(activity.contains("confirmedCondition.ifBlank { gradeLabel(result.conditionGrade) }"))
        assertTrue(activity.contains("confirmedCondition = MorleyVisionPolicy.conditionLabel(result.staffConfirmedConditionGrade)"))
        assertTrue(activity.contains("MorleyVisionPolicy.conditionLabel(result.staffConfirmedConditionGrade)"))
    }
}
