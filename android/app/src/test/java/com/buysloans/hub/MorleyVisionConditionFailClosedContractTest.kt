package com.buysloans.hub

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MorleyVisionConditionFailClosedContractTest {
    @Test
    fun missingConditionNeverDefaultsToFairForPricing() {
        val client = File("src/main/java/com/buysloans/hub/DeviceInspectionClient.kt").readText()
        val policy = File("src/main/java/com/buysloans/hub/MorleyVisionPolicy.kt").readText()

        assertFalse(client.contains("conditionGrade = MorleyVisionPolicy.clean(result.optString(\"condition_grade\")).ifBlank { \"C\" }"))
        assertFalse(client.contains("else -> ObservedCondition.FAIR"))
        assertTrue(policy.contains("isConditionVerified"))
        assertTrue(policy.contains("!isConditionVerified(inspection)"))
    }

    @Test
    fun edgeFunctionReturnsExplicitUnverifiedCondition() {
        val edge = File("../../supabase/functions/device-inspection/index.ts").readText()

        assertTrue(edge.contains("condition_grade: normaliseGrade(parsed.condition_grade) || \"UNVERIFIED\""))
    }
}
