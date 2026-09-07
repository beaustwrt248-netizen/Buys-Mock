package com.buysloans.nova;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class NovaControlCenterTruthContractTest {
    private static String source() throws Exception {
        return new String(Files.readAllBytes(
                Path.of("src/main/java/com/buysloans/nova/NovaControlCenterActivity.java")),
                StandardCharsets.UTF_8);
    }

    @Test
    public void controlCenterDoesNotPretendBackgroundTasksHaveProgress() throws Exception {
        String source = source();
        assertFalse(source.contains("Retailer scanning"));
        assertFalse(source.contains("addTask("));
        assertFalse(source.contains("ProgressBar"));
        assertFalse(source.matches("(?s).*addTask\\([^\\n]+,\\s*(78|62|54|86|69|91),.*"));
        assertTrue(source.contains("Live evidence checks"));
        assertTrue(source.contains("does not display fabricated completion percentages"));
    }

    @Test
    public void controlCenterUsesSessionPermissionAndApprovalFactsForStatus() throws Exception {
        String source = source();
        assertTrue(source.contains("NovaSessionBridge.api()"));
        assertTrue(source.contains("api.isSignedIn()"));
        assertTrue(source.contains("notificationPermissionGranted(this)"));
        assertTrue(source.contains("HUMAN-GATED"));
        assertTrue(source.contains("Approval required"));
        assertFalse(source.contains("metric(\"Catalogue\", \"LIVE\""));
        assertFalse(source.contains("metric(\"Support\", \"MONITORING\""));
        assertFalse(source.contains("addStatus(card, \"AI Model\", \"Online\""));
        assertFalse(source.contains("addStatus(card, \"Scheduled Tasks\", \"Active\""));
    }

    @Test
    public void currentMorleyEvidenceRemainsUserInitiatedAndAuthorised() throws Exception {
        String source = source();
        assertTrue(source.contains("assistant.answer(query)"));
        assertTrue(source.contains("Reading authorised Morley evidence"));
        assertTrue(source.contains("Guardian protected"));
        assertTrue(source.contains("risky changes require human approval"));
    }
}
