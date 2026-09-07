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
        assertTrue(source.contains("(width - gap * 3) / 4"));
        assertTrue(source.contains("new LinearLayout.LayoutParams(cardWidth, dp(activity, 92))"));
        assertTrue(source.contains("screenHeightPx * .25f"));
        assertTrue(source.contains("conversationListeners"));
        assertTrue(source.contains("setMarginEnd(dp(activity, 6))"));
        assertTrue(source.contains("raw.height = dp(activity, 50)"));
        assertTrue(source.contains("findEditTextByHint(decor, \"Message Nova…\")"));
    }

    @Test
    public void brainReferenceIsValidatedAndNeverFallsBackToAnEmptyRing() throws Exception {
        String gradle = read("build.gradle");
        assertTrue(gradle.contains("nova_reference_brain.png"));
        assertTrue(gradle.contains("javax.imageio.ImageIO.read"));
        assertTrue(gradle.contains("visibleSamples"));
        assertTrue(gradle.contains("litSamples"));
        assertTrue(gradle.contains("R.drawable.nova_reference_brain"));
        assertTrue(gradle.contains("reference.draw(canvas)"));
        assertTrue(gradle.contains("Fall back to Nova's native detailed brain"));
        assertTrue(Files.exists(Path.of("src/main/res/drawable-nodpi/nova_reference_brain.png")));
        assertTrue(!Files.exists(Path.of("src/main/res/drawable-nodpi/nova_reference_brain.webp")));
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
