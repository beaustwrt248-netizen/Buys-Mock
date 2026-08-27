package com.buysloans.admin

import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteConfigPolicyTest {
    @Test fun readsMaintenanceWithoutChangingOtherFlags() {
        val config = JSONArray("""[{"key":"feature_flags","value":{"maintenanceMode":false,"maintenanceMessage":"","ebayPricing":true,"deviceScanner":true}}]""")
        val current = maintenanceConfig(config)!!
        val payload = maintenanceUpdatePayload(current, true, " Planned maintenance ")
        val value = payload.getJSONObject("config_value")
        assertTrue(value.getBoolean("maintenanceMode"))
        assertEquals("Planned maintenance", value.getString("maintenanceMessage"))
        assertTrue(value.getBoolean("ebayPricing"))
        assertTrue(value.getBoolean("deviceScanner"))
    }

    @Test fun truncatesMaintenanceMessage() {
        val config = JSONArray("""[{"key":"feature_flags","value":{"maintenanceMode":false,"maintenanceMessage":""}}]""")
        val current = maintenanceConfig(config)!!
        val value = maintenanceUpdatePayload(current, false, "x".repeat(200)).getJSONObject("config_value")
        assertEquals(160, value.getString("maintenanceMessage").length)
        assertFalse(value.getBoolean("maintenanceMode"))
    }

    @Test fun ignoresReleaseRowsWhenFeatureFlagsMissing() {
        val config = JSONArray("""[{"key":"current_release","value":{"versionName":"2.14.6"}}]""")
        assertEquals(null, maintenanceConfig(config))
    }
}
