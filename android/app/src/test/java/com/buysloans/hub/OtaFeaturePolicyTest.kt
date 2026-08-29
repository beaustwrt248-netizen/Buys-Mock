package com.buysloans.hub

import org.json.JSONArray
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OtaFeaturePolicyTest {
    @Test fun otaDefaultsEnabledWhenConfigIsMissing() {
        assertTrue(OtaFeaturePolicy.enabledFromConfig(JSONArray()))
    }

    @Test fun otaDefaultsEnabledForLegacyFeatureFlags() {
        assertTrue(OtaFeaturePolicy.enabledFromConfig(JSONArray("""[{"key":"feature_flags","value":{"maintenanceMode":false}}]""")))
    }

    @Test fun otaCanBeExplicitlyDisabled() {
        assertFalse(OtaFeaturePolicy.enabledFromConfig(JSONArray("""[{"key":"feature_flags","value":{"otaEnabled":false}}]""")))
    }

    @Test fun otaCanBeExplicitlyEnabled() {
        assertTrue(OtaFeaturePolicy.enabledFromConfig(JSONArray("""[{"key":"feature_flags","value":{"otaEnabled":true}}]""")))
    }
}
