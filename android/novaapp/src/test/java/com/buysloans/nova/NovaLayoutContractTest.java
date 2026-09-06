package com.buysloans.nova;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class NovaLayoutContractTest {
    private static String read(String path) throws Exception {
        return new String(Files.readAllBytes(Path.of(path)), StandardCharsets.UTF_8);
    }

    @Test
    public void chatUsesDedicatedConversationScrollAndViewportQuickActions() throws Exception {
        String source = read("src/main/java/com/buysloans/nova/NovaApplication.java");
        assertTrue(source.contains("setVerticalScrollBarEnabled(true)"));
        assertTrue(source.contains("setScrollbarFadingEnabled(false)"));
        assertTrue(source.contains("row.getChildCount() != 4"));
        assertTrue(source.contains("new LinearLayout.LayoutParams(0, dp(activity, 116), 1f)"));
        assertTrue(source.contains("findEditTextByHint(decor, \"Message Nova…\")"));
    }

    @Test
    public void novaApplicationIsInstalled() throws Exception {
        String manifest = read("src/main/AndroidManifest.xml");
        assertTrue(manifest.contains("android:name=\".NovaApplication\""));
    }

    @Test
    public void otaChecksBypassStaleRawManifestCaches() throws Exception {
        String updater = read("src/main/java/com/buysloans/nova/UpdateManager.java");
        assertTrue(updater.contains("System.currentTimeMillis()"));
        assertTrue(updater.contains("Cache-Control"));
        assertTrue(updater.contains("no-cache, no-store, max-age=0"));
    }
}
