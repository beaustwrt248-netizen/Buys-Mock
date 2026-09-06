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

        assertTrue(source.contains("enum Tab { CHAT, INTELLIGENCE, UPDATES, ACCOUNT }"));
        assertTrue(source.contains("navButton(\"Chat\",Tab.CHAT)"));
        assertTrue(source.contains("navButton(\"Intelligence\",Tab.INTELLIGENCE)"));
        assertTrue(source.contains("navButton(\"Updates\",Tab.UPDATES)"));
        assertTrue(source.contains("navButton(\"Account\",Tab.ACCOUNT)"));
        assertTrue(source.contains("Message Nova…"));
        assertTrue(source.contains("Clear conversation"));
        assertTrue(engine.contains("Nova daily brief"));
        assertTrue(engine.contains("Nova knowledge overview"));
        assertTrue(engine.contains("lastKnowledgeQuery"));
        assertTrue(router.contains("DAILY_BRIEF"));
        assertTrue(router.contains("KNOWLEDGE"));
        assertTrue(router.contains("keep going"));
        assertTrue(router.contains("show me more"));
    }
}
