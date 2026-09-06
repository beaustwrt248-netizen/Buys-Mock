package com.buysloans.nova;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;

import androidx.core.content.FileProvider;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Locale;

final class UpdateManager {
    private static final String RELEASE_PREFIX = "https://github.com/beaustwrt248-netizen/Buys-Mock/releases/download/nova-v";

    interface Listener {
        void onStatus(String message);
        void onUpdateAvailable(UpdateInfo info);
        void onUpToDate();
        void onError(String message);
    }

    static final class UpdateInfo {
        final int versionCode;
        final String versionName;
        final String apkUrl;
        final String sha256;
        final boolean mandatory;
        final String notes;

        UpdateInfo(int versionCode, String versionName, String apkUrl, String sha256, boolean mandatory, String notes) {
            this.versionCode = versionCode;
            this.versionName = versionName;
            this.apkUrl = apkUrl;
            this.sha256 = sha256;
            this.mandatory = mandatory;
            this.notes = notes;
        }
    }

    private final Activity activity;
    private final Listener listener;

    UpdateManager(Activity activity, Listener listener) {
        this.activity = activity;
        this.listener = listener;
    }

    void checkForUpdates() {
        listener.onStatus("Checking for Nova updates…");
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                String separator = BuildConfig.OTA_MANIFEST_URL.contains("?") ? "&" : "?";
                String cacheBuster = "nova=" + BuildConfig.VERSION_CODE + "&ts=" + System.currentTimeMillis();
                URL manifestUrl = new URL(BuildConfig.OTA_MANIFEST_URL + separator + cacheBuster);
                connection = (HttpURLConnection) manifestUrl.openConnection();
                connection.setConnectTimeout(12_000);
                connection.setReadTimeout(12_000);
                connection.setUseCaches(false);
                connection.setDefaultUseCaches(false);
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestProperty("Cache-Control", "no-cache, no-store, max-age=0");
                connection.setRequestProperty("Pragma", "no-cache");
                connection.setRequestProperty("Expires", "0");
                int response = connection.getResponseCode();
                if (response != HttpURLConnection.HTTP_OK) {
                    throw new IllegalStateException("Update service returned HTTP " + response);
                }
                String json = readFully(connection.getInputStream());
                JSONObject object = new JSONObject(json);
                UpdateInfo info = new UpdateInfo(
                        object.getInt("versionCode"),
                        object.getString("versionName").trim(),
                        object.getString("apkUrl").trim(),
                        object.getString("sha256").toLowerCase(Locale.US),
                        object.optBoolean("mandatory", false),
                        object.optString("notes", "Nova update available."));

                if (info.versionCode < 0 || info.versionName.isBlank()) {
                    throw new IllegalStateException("Update manifest has an invalid release identity");
                }
                if (!info.sha256.matches("[0-9a-f]{64}")) {
                    throw new IllegalStateException("Update manifest has an invalid SHA-256 digest");
                }
                if (info.versionCode > BuildConfig.VERSION_CODE && !info.apkUrl.startsWith(RELEASE_PREFIX)) {
                    throw new SecurityException("Update manifest points outside the approved Nova release channel");
                }
                activity.runOnUiThread(() -> {
                    if (info.versionCode > BuildConfig.VERSION_CODE) {
                        listener.onUpdateAvailable(info);
                    } else {
                        listener.onUpToDate();
                    }
                });
            } catch (Exception error) {
                activity.runOnUiThread(() -> listener.onError("Update check failed: " + safeMessage(error)));
            } finally {
                if (connection != null) connection.disconnect();
            }
        }, "nova-update-check").start();
    }

    void downloadAndInstall(UpdateInfo info) {
        listener.onStatus("Downloading Nova " + info.versionName + "…");
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                if (!info.apkUrl.startsWith(RELEASE_PREFIX)) {
                    throw new SecurityException("Update URL is outside the approved Nova release channel");
                }
                File updateDir = new File(activity.getCacheDir(), "updates");
                if (!updateDir.exists() && !updateDir.mkdirs()) {
                    throw new IllegalStateException("Could not create update directory");
                }
                File apk = new File(updateDir, "Nova-AI-" + info.versionName + ".apk");
                connection = (HttpURLConnection) new URL(info.apkUrl).openConnection();
                connection.setConnectTimeout(20_000);
                connection.setReadTimeout(30_000);
                connection.setInstanceFollowRedirects(true);
                int response = connection.getResponseCode();
                if (response < 200 || response >= 300) {
                    throw new IllegalStateException("APK download returned HTTP " + response);
                }
                try (InputStream in = new BufferedInputStream(connection.getInputStream());
                     FileOutputStream out = new FileOutputStream(apk)) {
                    byte[] buffer = new byte[16 * 1024];
                    int read;
                    long total = 0;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                        total += read;
                    }
                    if (total == 0) throw new IllegalStateException("Downloaded APK is empty");
                }

                String actual = sha256(apk);
                if (!actual.equals(info.sha256)) {
                    apk.delete();
                    throw new SecurityException("Downloaded APK failed SHA-256 verification");
                }
                activity.runOnUiThread(() -> installVerifiedApk(apk));
            } catch (Exception error) {
                activity.runOnUiThread(() -> listener.onError("Update download failed: " + safeMessage(error)));
            } finally {
                if (connection != null) connection.disconnect();
            }
        }, "nova-update-download").start();
    }

    private void installVerifiedApk(File apk) {
        if (!activity.getPackageManager().canRequestPackageInstalls()) {
            listener.onStatus("Allow Nova to install updates, then tap Install Update again.");
            Intent settings = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + activity.getPackageName()));
            activity.startActivity(settings);
            return;
        }
        Uri uri = FileProvider.getUriForFile(activity,
                activity.getPackageName() + ".fileprovider", apk);
        Intent install = new Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(install);
    }

    private static String readFully(InputStream input) throws Exception {
        StringBuilder builder = new StringBuilder();
        byte[] buffer = new byte[8 * 1024];
        int read;
        while ((read = input.read(buffer)) != -1) {
            builder.append(new String(buffer, 0, read, java.nio.charset.StandardCharsets.UTF_8));
        }
        return builder.toString();
    }

    private static String sha256(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (FileInputStream input = new FileInputStream(file)) {
            byte[] buffer = new byte[16 * 1024];
            int read;
            while ((read = input.read(buffer)) != -1) digest.update(buffer, 0, read);
        }
        StringBuilder hex = new StringBuilder(64);
        for (byte value : digest.digest()) hex.append(String.format(Locale.US, "%02x", value));
        return hex.toString();
    }

    private static String safeMessage(Throwable error) {
        String message = error.getMessage();
        return message == null || message.isBlank() ? error.getClass().getSimpleName() : message;
    }
}
