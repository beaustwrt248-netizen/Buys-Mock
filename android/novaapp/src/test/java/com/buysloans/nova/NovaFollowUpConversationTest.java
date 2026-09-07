package com.buysloans.nova;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class NovaFollowUpConversationTest {
    @Test public void firstMoreTapExpandsIntroInsteadOfDroppingContext() throws Exception {
        NovaAssistantEngine engine = new NovaAssistantEngine(new NovaApiClient());
        String answer = engine.answer("show me more");
        assertTrue(answer.contains("I can go further than the quick actions"));
        assertTrue(answer.contains("keep the same thread"));
        assertFalse(answer.contains("don’t yet have enough context"));
    }

    @Test public void followUpAfterGreetingKeepsConversationThread() throws Exception {
        NovaAssistantEngine engine = new NovaAssistantEngine(new NovaApiClient());
        engine.answer("hello");
        String answer = engine.answer("tell me more");
        assertTrue(answer.contains("previous answer") || answer.contains("I can go further"));
        assertFalse(answer.contains("don’t yet have enough context"));
    }

    @Test public void resetClearsStoredConversationButMoreStillHasUsefulIntroContext() throws Exception {
        NovaAssistantEngine engine = new NovaAssistantEngine(new NovaApiClient());
        engine.answer("hello");
        engine.resetContext();
        String answer = engine.answer("more");
        assertTrue(answer.contains("I can go further than the quick actions"));
    }
}
