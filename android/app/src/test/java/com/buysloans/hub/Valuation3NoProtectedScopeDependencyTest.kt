package com.buysloans.hub

import org.junit.Assert.assertNotNull
import org.junit.Test

/** Compile-time sentinel: final valuation composition remains independently testable in the app module. */
class Valuation3NoProtectedScopeDependencyTest {
    @Test fun completeValuationEngineIsAvailableInCoreAppModule() {
        assertNotNull(CompleteValuationDecision)
        assertNotNull(ValuationDecisionCoordinator)
        assertNotNull(DealIntelligenceDecisionAdapter)
        assertNotNull(TargetMarginPolicy)
    }
}
