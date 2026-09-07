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

    @Test public void routesEnhancedNovaFeatures() {
        assertEquals(IntentRouter.Intent.DAILY_BRIEF, IntentRouter.classify("Give me the daily brief"));
        assertEquals(IntentRouter.Intent.DAILY_BRIEF, IntentRouter.classify("What's happening today?"));
        assertEquals(IntentRouter.Intent.KNOWLEDGE, IntentRouter.classify("Show me the Nova knowledge base"));
        assertEquals(IntentRouter.Intent.ATTENTION, IntentRouter.classify("Any blockers right now?"));
        assertEquals(IntentRouter.Intent.PERFORMANCE, IntentRouter.classify("How is Morley doing?"));
        assertEquals(IntentRouter.Intent.CATALOGUE, IntentRouter.classify("Are there missing specs or duplicate devices?"));
        assertEquals(IntentRouter.Intent.RELEASES, IntentRouter.classify("What's new in the latest build?"));
    }

    @Test public void routesGeneralConversationWithoutForcingMorleyIntent() {
        assertEquals(IntentRouter.Intent.GREETING, IntentRouter.classify("Good evening Nova"));
        assertEquals(IntentRouter.Intent.SMALL_TALK, IntentRouter.classify("How are you?"));
        assertEquals(IntentRouter.Intent.SMALL_TALK, IntentRouter.classify("How was your day?"));
        assertEquals(IntentRouter.Intent.SMALL_TALK, IntentRouter.classify("Thanks Nova"));
        assertEquals(IntentRouter.Intent.SMALL_TALK, IntentRouter.classify("Tell me a joke"));
        assertEquals(IntentRouter.Intent.SMALL_TALK, IntentRouter.classify("Who are you?"));
    }

    @Test public void prefersSpecificMorleyDomainsOverGenericOpinionPhrase() {
        assertEquals(IntentRouter.Intent.SMALL_TALK, IntentRouter.classify("What do you think?"));
        assertEquals(IntentRouter.Intent.SUPPORT, IntentRouter.classify("What do you think about the support tickets?"));
        assertEquals(IntentRouter.Intent.CATALOGUE, IntentRouter.classify("What do you think about the catalogue gaps?"));
        assertEquals(IntentRouter.Intent.PERFORMANCE, IntentRouter.classify("What do you think about Morley sales?"));
        assertEquals(IntentRouter.Intent.RELEASES, IntentRouter.classify("What do you think about the latest release?"));
    }

    @Test public void recognisesShortContextualFollowUps() {
        assertTrue(IntentRouter.isFollowUp("why?"));
        assertTrue(IntentRouter.isFollowUp("tell me more"));
        assertTrue(IntentRouter.isFollowUp("what about that?"));
        assertTrue(IntentRouter.isFollowUp("and which one is worst?"));
        assertTrue(IntentRouter.isFollowUp("keep going"));
        assertTrue(IntentRouter.isFollowUp("show me more"));
        assertFalse(IntentRouter.isFollowUp("write me a detailed unrelated report about the moon and its geology"));
    }

    @Test public void protectedQuestionRoutesToGuardianRatherThanPretendingToAct() {
        assertEquals(IntentRouter.Intent.GUARDIAN, IntentRouter.classify("Apply the protected repair approval"));
    }

    @Test public void unknownDoesNotPretendToUnderstand() {
        assertEquals(IntentRouter.Intent.UNKNOWN, IntentRouter.classify("write me a poem about the moon"));
    }
}
