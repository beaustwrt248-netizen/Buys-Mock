package com.buysloans.nova;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public final class NovaResilienceContractTest {
    private static String read(String path) throws Exception {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }

    @Test
    public void rotationKeepsActivitySessionAndKeyboardResizesContent() throws Exception {
        String manifest = read("src/main/AndroidManifest.xml");
        assertTrue(manifest.contains("android:configChanges=\"orientation|screenSize|keyboardHidden\""));
        assertTrue(manifest.contains("android:windowSoftInputMode=\"adjustResize\""));
    }

    @Test
    public void transientNetworkRetryOnlyAppliesToReadRequests() throws Exception {
        String api = read("src/main/java/com/buysloans/nova/NovaApiClient.java");
        assertTrue(api.contains("if (!\"GET\".equals(method)) return requestOnce"));
        assertTrue(api.contains("Thread.sleep(350L)"));
        assertTrue(api.contains("Nova could not reach Morley. Check your connection and try again."));
        assertFalse(api.contains("if (\"POST\".equals(method))"));
    }

    @Test
    public void verifiedUpdateResumesAfterUnknownSourcesPermission() throws Exception {
        String updater = read("src/main/java/com/buysloans/nova/UpdateManager.java");
        String initializer = read("src/main/java/com/buysloans/nova/NovaOperatorInitializer.java");
        assertTrue(updater.contains("rememberVerifiedUpdate"));
        assertTrue(updater.contains("resumePendingInstall"));
        assertTrue(updater.contains("expected.equals(sha256(apk))"));
        assertTrue(updater.contains("Installation will resume automatically when you return."));
        assertTrue(initializer.contains("UpdateManager.resumePendingInstall(activity)"));
    }

    @Test
    public void resilienceReleaseIdentityAdvancesExactlyOneVersion() throws Exception {
        String gradle = read("build.gradle");
        assertTrue(gradle.contains("versionCode 22"));
        assertTrue(gradle.contains("versionName '0.3.18'"));
    }
}
