package com.buysloans.nova;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertTrue;

public class NovaMultiModelContractTest {
    private static String readUtf8(String path) throws Exception {
        return new String(Files.readAllBytes(Path.of(path)), StandardCharsets.UTF_8);
    }

    @Test public void nativeGeneralChatUsesGuardedOrchestrator() throws Exception {
        String api = readUtf8("src/main/java/com/buysloans/nova/NovaApiClient.java");
        String engine = readUtf8("src/main/java/com/buysloans/nova/NovaAssistantEngine.java");
        String gradle = readUtf8("build.gradle");

        assertTrue(api.contains("edge(\"nova-orchestrator\""));
        assertTrue(api.contains("JSONObject orchestrate(String prompt, String mode)"));
        assertTrue(api.contains("ORCHESTRATOR_READ_TIMEOUT_MS = 105_000"));
        assertTrue(api.contains("path.startsWith(\"/functions/v1/nova-orchestrator\")"));
        assertTrue(api.contains("connection.setReadTimeout(readTimeoutFor(path))"));
        assertTrue(engine.contains("multiModelAnswer(q, false)"));
        assertTrue(engine.contains("multiModelAnswer(q, true)"));
        assertTrue(engine.contains("ensemble ? \"ensemble\" : \"auto\""));
        assertTrue(engine.contains("ask all models"));
        assertTrue(engine.contains("cross check with gpt"));
        assertTrue(engine.contains("Protected approvals, repairs, pricing decisions and releases still stay behind their existing human boundaries"));
        assertTrue(engine.contains("I haven’t guessed an answer or claimed a model result I didn’t receive"));

        // An explicit ensemble request must win before broad specialist keyword routing.
        int explicitGuard = engine.indexOf("if (isExplicitMultiModelRequest(q))");
        int classify = engine.indexOf("IntentRouter.Intent intent = IntentRouter.classify(q)");
        assertTrue(explicitGuard >= 0);
        assertTrue(classify >= 0);
        assertTrue(explicitGuard < classify);
        assertTrue(engine.contains("return remember(q, IntentRouter.Intent.UNKNOWN, multiModelAnswer(q, false));"));

        assertTrue(gradle.contains("versionCode 31"));
        assertTrue(gradle.contains("versionName '0.3.27'"));
    }
}
