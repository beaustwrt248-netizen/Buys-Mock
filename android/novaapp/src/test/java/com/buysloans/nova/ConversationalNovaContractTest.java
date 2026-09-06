package com.buysloans.nova;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ConversationalNovaContractTest {
    private static String source(String file) throws Exception {
        String rel = "src/main/java/com/buysloans/nova/" + file;
        Path[] candidates = new Path[]{
                Paths.get(rel),
                Paths.get("novaapp", rel),
                Paths.get("android", "novaapp", rel)
        };
        for (Path path : candidates) {
            if (Files.exists(path)) return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        }
        throw new IllegalStateException("Nova source file not found: " + file + " from " + System.getProperty("user.dir"));
    }

    @Test public void assistantUsesAuthorisedKnowledgeAndLearningSources() throws Exception {
        String api = source("NovaApiClient.java");
        assertTrue(api.contains("/functions/v1/"));
        assertTrue(api.contains("nova-knowledge"));
        assertTrue(api.contains("nova-learning"));
        assertTrue(api.contains("session.accessToken"));
        assertFalse(api.contains("SUPABASE_SERVICE_ROLE_KEY"));
    }

    @Test public void conversationPreservesProtectedBoundariesAndIsSessionOnly() throws Exception {
        String activity = source("MainActivity.java");
        String engine = source("NovaAssistantEngine.java");
        assertTrue(activity.contains("Conversation context stays in this app session only"));
        assertTrue(engine.contains("protected actions remain human-approved"));
        assertFalse(activity.contains("SharedPreferences"));
        assertFalse(activity.contains("FileOutputStream"));
    }
}
