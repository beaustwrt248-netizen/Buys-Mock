package com.buysloans.nova;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class IntentRouterTest {
    @Test public void routesCoreMorleyDomains() {
        assertEquals(IntentRouter.Intent.ATTENTION, IntentRouter.classify("What needs my attention?"));
        assertEquals(IntentRouter.Intent.ATTENTION, IntentRouter.classify("Anything urgent right now?"));
        assertEquals(IntentRouter.Intent.GUARDIAN, IntentRouter.classify("What needs Guardian approval?"));
        assertEquals(IntentRouter.Intent.SUPPORT, IntentRouter.classify("How is the support queue?"));
        assertEquals(IntentRouter.Intent.INVENTORY, IntentRouter.classify("Show inventory health"));
        assertEquals(IntentRouter.Intent.PERFORMANCE, IntentRouter.classify("How are sales and profit?"));
        assertEquals(IntentRouter.Intent.RELEASES, IntentRouter.classify("Is there a new Nova APK release?"));
        assertEquals(IntentRouter.Intent.CATALOGUE, IntentRouter.classify("How healthy is the device catalogue?"));
        assertEquals(IntentRouter.Intent.LEARNING, IntentRouter.classify("What is Nova learning from outcomes?"));
    }

    @Test public void routesGeneralConversationWithoutForcingMorleyIntent() {
        assertEquals(IntentRouter.Intent.GREETING, IntentRouter.classify("Good evening Nova"));
        assertEquals(IntentRouter.Intent.SMALL_TALK, IntentRouter.classify("How are you?"));
        assertEquals(IntentRouter.Intent.SMALL_TALK, IntentRouter.classify("Thanks Nova"));
        assertEquals(IntentRouter.Intent.SMALL_TALK, IntentRouter.classify("Tell me a joke"));
        assertEquals(IntentRouter.Intent.SMALL_TALK, IntentRouter.classify("Who are you?"));
    }

    @Test public void recognisesShortContextualFollowUps() {
        assertTrue(IntentRouter.isFollowUp("why?"));
        assertTrue(IntentRouter.isFollowUp("tell me more"));
        assertTrue(IntentRouter.isFollowUp("what about that?"));
        assertTrue(IntentRouter.isFollowUp("and which one is worst?"));
        assertFalse(IntentRouter.isFollowUp("write me a detailed unrelated report about the moon and its geology"));
    }

    @Test public void protectedQuestionRoutesToGuardianRatherThanPretendingToAct() {
        assertEquals(IntentRouter.Intent.GUARDIAN, IntentRouter.classify("Apply the protected repair approval"));
    }

    @Test public void unknownDoesNotPretendToUnderstand() {
        assertEquals(IntentRouter.Intent.UNKNOWN, IntentRouter.classify("write me a poem about the moon"));
    }
}
