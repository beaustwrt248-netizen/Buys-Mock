package com.buysloans.nova;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.JSONObject;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    public void optionalStartupHelpersCannotCrashMainActivity() throws Exception {
        String initializer = read("src/main/java/com/buysloans/nova/NovaOperatorInitializer.java");
        assertTrue(initializer.contains("Operator extras must never prevent Nova itself from launching."));
        assertTrue(initializer.contains("Lifecycle helpers fail open so MainActivity can still render."));
        assertTrue(initializer.contains("A stale or malformed pending installer must never crash startup."));
        assertTrue(initializer.contains("activity.isFinishing() || activity.isDestroyed()"));
    }

    @Test
    public void releaseIdentityAdvancesExactlyOneVersionBeyondPublishedOta() throws Exception {
        String gradle = read("build.gradle");
        JSONObject published = new JSONObject(read("../../nova/nova-update.json"));

        Matcher code = Pattern.compile("\\bversionCode\\s+(\\d+)").matcher(gradle);
        Matcher name = Pattern.compile("\\bversionName\\s+['\"]([^'\"]+)['\"]").matcher(gradle);
        assertTrue("Nova build.gradle must declare versionCode", code.find());
        assertTrue("Nova build.gradle must declare versionName", name.find());

        int currentCode = Integer.parseInt(code.group(1));
        String currentName = name.group(1).trim();
        int publishedCode = published.getInt("versionCode");
        String publishedName = published.getString("versionName").trim();

        assertEquals("Nova release must advance exactly one versionCode beyond published OTA",
                publishedCode + 1, currentCode);
        assertFalse("Nova release versionName must differ from the already-published OTA",
                currentName.equals(publishedName));
    }
}
