package com.buysloans.nova;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class IntentRouterTest {
    @Test public void routesCoreMorleyDomains() {
        assertEquals(IntentRouter.Intent.GUARDIAN, IntentRouter.classify("What needs Guardian approval?"));
        assertEquals(IntentRouter.Intent.SUPPORT, IntentRouter.classify("How is the support queue?"));
        assertEquals(IntentRouter.Intent.INVENTORY, IntentRouter.classify("Show inventory health"));
        assertEquals(IntentRouter.Intent.PERFORMANCE, IntentRouter.classify("How are sales and profit?"));
        assertEquals(IntentRouter.Intent.RELEASES, IntentRouter.classify("Is there a new Nova APK release?"));
    }

    @Test public void unknownDoesNotPretendToUnderstand() {
        assertEquals(IntentRouter.Intent.UNKNOWN, IntentRouter.classify("write me a poem about the moon"));
    }
}
