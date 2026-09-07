package com.buysloans.nova;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

public class NovaAndroidOperatorContractTest {
    private static String read(String relative) throws Exception {
        return Files.readString(Path.of(relative));
    }

    @Test
    public void visionUsesAdminOnlyServerFunctionAndNoPhotoPersistenceContract() throws Exception {
        String api = read("src/main/java/com/buysloans/nova/NovaApiClient.java");
        String vision = read("src/main/java/com/buysloans/nova/NovaVisionActivity.java");
        String helper = read("src/main/java/com/buysloans/nova/NovaAndroidOperator.java");
        assertTrue(api.contains("edge(\"nova-vision\""));
        assertTrue(vision.contains("NovaAndroidOperator.imageDataUrl"));
        assertTrue(vision.contains("api.vision(dataUrl, hintText)"));
        assertTrue(helper.contains("Photo privacy: not stored by Nova Vision"));
        assertTrue(helper.contains("MAX_IMAGE_BYTES = 6 * 1024 * 1024"));
    }

    @Test
    public void proactiveAlertsAreLocalAuthenticatedAndFifteenMinuteMinimum() throws Exception {
        String manifest = read("src/main/AndroidManifest.xml");
        String helper = read("src/main/java/com/buysloans/nova/NovaAndroidOperator.java");
        String worker = read("src/main/java/com/buysloans/nova/NovaAlertWorker.java");
        assertTrue(manifest.contains("android.permission.POST_NOTIFICATIONS"));
        assertTrue(manifest.contains("android.permission.BIND_JOB_SERVICE"));
        assertTrue(helper.contains("15 * 60 * 1000L"));
        assertTrue(worker.contains("api.restoreSession(refresh)"));
        assertTrue(worker.contains("terminalGuardian"));
        assertTrue(worker.contains("terminalSupport"));
    }

    @Test
    public void controlCentreAndVisionActivitiesAreNotExported() throws Exception {
        String manifest = read("src/main/AndroidManifest.xml");
        assertTrue(manifest.contains("android:name=\".NovaVisionActivity\"\n            android:exported=\"false\""));
        assertTrue(manifest.contains("android:name=\".NovaControlCenterActivity\"\n            android:exported=\"false\""));
        String ui = read("src/main/java/com/buysloans/nova/NovaOperatorUi.java");
        assertTrue(ui.contains("Open Nova control centre"));
        assertTrue(ui.contains("NovaSessionBridge.attach"));
    }

    @Test
    public void androidLiveSourcesPageInsteadOfUsingFixedRecentLimits() throws Exception {
        String api = read("src/main/java/com/buysloans/nova/NovaApiClient.java");
        assertTrue(api.contains("sales_records?select=id,acquired_cost"));
        assertTrue(api.contains("support_tickets?select=id,status,priority"));
        assertTrue(api.contains("getAllPages"));
        assertTrue(api.contains("image_reference_url"));
    }
}
