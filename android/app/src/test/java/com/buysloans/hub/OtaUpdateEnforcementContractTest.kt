package com.buysloans.hub

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class OtaUpdateEnforcementContractTest {
    @Test
    fun `update scheduler checks every six hours`() {
        val source = File("src/main/java/com/buysloans/hub/UpdateCheckWorker.kt").readText()
        assertTrue(source.contains("PeriodicWorkRequestBuilder<UpdateCheckWorker>(6, TimeUnit.HOURS)"))
    }

    @Test
    fun `any verified newer ota opens mandatory update gate`() {
        val coordinator = File("src/main/java/com/buysloans/hub/ReleasePolicyCoordinator.kt").readText()
        assertTrue(coordinator.contains("policy.requiresMandatoryUpdate() || update != null"))

        val gate = File("src/main/java/com/buysloans/hub/MandatoryUpdateActivity.kt").readText()
        assertTrue(gate.contains("!currentPolicy.requiresMandatoryUpdate() && available == null"))
        assertTrue(gate.contains("Update before continuing so every device uses the same supported software version."))
    }
}
