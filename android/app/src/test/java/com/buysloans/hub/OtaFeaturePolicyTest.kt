package com.buysloans.hub

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OtaFeaturePolicyTest {
    @Test fun otaDefaultsEnabledWhenConfigIsMissing() {
        assertTrue(otaEnabledValue(null))
    }

    @Test fun otaDefaultsEnabledForLegacyFeatureFlags() {
        assertTrue(otaEnabledValue(null))
    }

    @Test fun otaCanBeExplicitlyDisabled() {
        assertFalse(otaEnabledValue(false))
    }

    @Test fun otaCanBeExplicitlyEnabled() {
        assertTrue(otaEnabledValue(true))
    }
}
