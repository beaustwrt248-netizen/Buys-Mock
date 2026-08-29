package com.buysloans.hub

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OtaFeaturePolicyTest {
    @Test fun otaDefaultsEnabledWhenConfigIsMissing() {
        assertTrue(OtaFeaturePolicy.enabledValue(null))
    }

    @Test fun otaDefaultsEnabledForLegacyFeatureFlags() {
        assertTrue(OtaFeaturePolicy.enabledValue(null))
    }

    @Test fun otaCanBeExplicitlyDisabled() {
        assertFalse(OtaFeaturePolicy.enabledValue(false))
    }

    @Test fun otaCanBeExplicitlyEnabled() {
        assertTrue(OtaFeaturePolicy.enabledValue(true))
    }
}
