package com.buysloans.nova;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertTrue;

public class ConversationalNovaContractTest {
    private static String readUtf8(String path) throws Exception {
        return new String(Files.readAllBytes(Path.of(path)), StandardCharsets.UTF_8);
    }

    @Test public void conversationAndNavigationFeaturesRemainPresent() throws Exception {
        String source = readUtf8("src/main/java/com/buysloans/nova/MainActivity.java");
        String engine = readUtf8("src/main/java/com/buysloans/nova/NovaAssistantEngine.java");
        String router = readUtf8("src/main/java/com/buysloans/nova/IntentRouter.java");
        String gradle = readUtf8("build.gradle");

        assertTrue(source.contains("navButton(\"Chat\",Tab.CHAT)"));
        assertTrue(source.contains("navButton(\"Intelligence\",Tab.INTELLIGENCE)"));
        assertTrue(source.contains("navButton(\"Account\",Tab.ACCOUNT)"));
        assertTrue(gradle.contains("private enum Tab { CHAT, INTELLIGENCE, ACCOUNT }"));
        assertTrue(gradle.contains("conversationScroll.fullScroll(View.FOCUS_DOWN)"));
        assertTrue(gradle.contains("addOtaSection();\\n        footer();\\n    }\\n\\n    private void sendQuestion"));
        assertTrue(source.contains("Message Nova…"));
        assertTrue(source.contains("Clear conversation"));

        assertTrue(engine.contains("lastQuestion"));
        assertTrue(engine.contains("lastAnswer"));
        assertTrue(engine.contains("contextualFollowUp"));
        assertTrue(engine.contains("capabilityContinuation"));
        assertTrue(engine.contains("show me more"));
        assertTrue(engine.contains("tell me more"));
        assertTrue(engine.contains("what about that"));
        assertTrue(engine.contains("cancelled"));
        assertTrue(engine.contains("dismissed"));

        assertTrue(router.contains("DAILY_BRIEF"));
        assertTrue(router.contains("KNOWLEDGE"));
        assertTrue(router.contains("keep going"));
        assertTrue(router.contains("show me more"));
    }
}
