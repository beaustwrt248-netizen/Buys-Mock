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
        assertEquals(IntentRouter.Intent.CATALOGUE, IntentRouter.classify("How healthy is the device catalogue?"));
        assertEquals(IntentRouter.Intent.CATALOGUE, IntentRouter.classify("Which records are missing model numbers?"));
        assertEquals(IntentRouter.Intent.LEARNING, IntentRouter.classify("What is Nova learning from outcomes?"));
        assertEquals(IntentRouter.Intent.LEARNING, IntentRouter.classify("How is valuation forecast error?"));
    }

    @Test public void protectedQuestionRoutesToGuardianRatherThanPretendingToAct() {
        assertEquals(IntentRouter.Intent.GUARDIAN, IntentRouter.classify("Apply the protected repair approval"));
    }

    @Test public void unknownDoesNotPretendToUnderstand() {
        assertEquals(IntentRouter.Intent.UNKNOWN, IntentRouter.classify("write me a poem about the moon"));
    }
}
