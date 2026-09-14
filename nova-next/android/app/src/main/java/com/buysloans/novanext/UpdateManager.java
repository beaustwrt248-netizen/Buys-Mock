package com.buysloans.novanext;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

final class UpdateManager {
    private static final String RELEASE_PREFIX =
            "https://github.com/beaustwrt248-netizen/Buys-Mock/releases/download/nova-next-v";
    private static final String SAFE_VERSION_NAME = "[0-9A-Za-z][0-9A-Za-z._-]*";
    private static final String UPDATE_PREFS = "nova_next_verified_update";
    private static final String PENDING_PATH = "pending_path";
    private static final String PENDING_SHA = "pending_sha";
    private static final String PENDING_CODE = "pending_code";

    interface Listener {
        void onStatus(String message);
        void onUpdateAvailable(UpdateInfo info);
        void onUpToDate();
        void onError(String message);
    }

    static final class UpdateInfo {
        final String appId;
        final String channel;
        final String packageName;
        final int versionCode;
        final String versionName;
        final String downloadUrl;
        final String sha256;
        final boolean mandatory;
        final String notes;

        UpdateInfo(String appId, String channel, String packageName, int versionCode,
                   String versionName, String downloadUrl, String sha256,
                   boolean mandatory, String notes) {
            this.appId = appId;
            this.channel = channel;
            this.packageName = packageName;
            this.versionCode = versionCode;
            this.versionName = versionName;
            this.downloadUrl = downloadUrl;
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
        listener.onStatus("Checking for Nova Next updates…");
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                String separator = BuildConfig.NOVA_OTA_MANIFEST_URL.contains("?") ? "&" : "?";
                String cacheBuster = "version=" + BuildConfig.VERSION_CODE + "&ts=" + System.currentTimeMillis();
                URL manifestUrl = new URL(BuildConfig.NOVA_OTA_MANIFEST_URL + separator + cacheBuster);
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

                JSONObject object = new JSONObject(readFully(connection.getInputStream()));
                UpdateInfo info = new UpdateInfo(
                        object.getString("appId").trim(),
                        object.getString("channel").trim(),
                        object.getString("packageName").trim(),
                        object.getInt("versionCode"),
                        object.getString("versionName").trim(),
                        object.optString("downloadUrl", "").trim(),
                        object.optString("sha256", "").toLowerCase(Locale.US).trim(),
                        object.optBoolean("mandatory", false),
                        object.optString("notes", "Nova Next update available.").trim());

                validateIdentity(info);
                boolean newer = info.versionCode > BuildConfig.VERSION_CODE;
                if (newer) validateDownloadMetadata(info);

                activity.runOnUiThread(() -> {
                    if (newer) {
                        listener.onUpdateAvailable(info);
                    } else {
                        clearObsoletePending(activity);
                        listener.onUpToDate();
                    }
                });
            } catch (Exception error) {
                activity.runOnUiThread(() -> listener.onError("Update check failed: " + safeMessage(error)));
            } finally {
                if (connection != null) connection.disconnect();
            }
        }, "nova-next-update-check").start();
    }

    void downloadAndInstall(UpdateInfo info) {
        try {
            validateIdentity(info);
            validateDownloadMetadata(info);
            if (info.versionCode <= BuildConfig.VERSION_CODE) {
                listener.onUpToDate();
                return;
            }
        } catch (Exception error) {
            listener.onError("Update rejected: " + safeMessage(error));
            return;
        }

        listener.onStatus("Downloading Nova Next " + info.versionName + "…");
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                File updateDir = new File(activity.getCacheDir(), "nova-next-updates");
                if (!updateDir.exists() && !updateDir.mkdirs()) {
                    throw new IllegalStateException("Could not create update directory");
                }
                File canonicalDir = updateDir.getCanonicalFile();
                File apk = new File(canonicalDir, "Nova-Next-" + info.versionName + ".apk").getCanonicalFile();
                if (!canonicalDir.equals(apk.getParentFile())) {
                    throw new SecurityException("Update file path escaped the approved cache directory");
                }

                if (apk.isFile() && info.sha256.equals(sha256(apk))) {
                    verifyApkIdentity(apk, info.versionCode);
                    rememberVerifiedUpdate(activity, apk, info.sha256, info.versionCode);
                    activity.runOnUiThread(() -> installVerifiedApk(apk));
                    return;
                }
                if (apk.exists() && !apk.delete()) {
                    throw new IllegalStateException("Could not replace an invalid cached update");
                }

                connection = (HttpURLConnection) new URL(info.downloadUrl).openConnection();
                connection.setConnectTimeout(20_000);
                connection.setReadTimeout(30_000);
                connection.setInstanceFollowRedirects(true);
                connection.setUseCaches(false);
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
                verifyApkIdentity(apk, info.versionCode);
                rememberVerifiedUpdate(activity, apk, info.sha256, info.versionCode);
                activity.runOnUiThread(() -> installVerifiedApk(apk));
            } catch (Exception error) {
                activity.runOnUiThread(() -> listener.onError("Update download failed: " + safeMessage(error)));
            } finally {
                if (connection != null) connection.disconnect();
            }
        }, "nova-next-update-download").start();
    }

    private void validateIdentity(UpdateInfo info) {
        if (!BuildConfig.NOVA_OTA_APP_ID.equals(info.appId)) {
            throw new SecurityException("Update app identity mismatch");
        }
        if (!BuildConfig.NOVA_OTA_CHANNEL.equals(info.channel)) {
            throw new SecurityException("Update channel mismatch");
        }
        if (!activity.getPackageName().equals(info.packageName)) {
            throw new SecurityException("Update package identity mismatch");
        }
        if (info.versionCode <= 0 || info.versionName.isBlank()) {
            throw new IllegalStateException("Update manifest has an invalid release identity");
        }
        if (!info.versionName.matches(SAFE_VERSION_NAME)) {
            throw new SecurityException("Update manifest has an unsafe version name");
        }
    }

    private static void validateDownloadMetadata(UpdateInfo info) {
        if (!info.sha256.matches("[0-9a-f]{64}")) {
            throw new IllegalStateException("Update manifest has an invalid SHA-256 digest");
        }
        if (!info.downloadUrl.startsWith(RELEASE_PREFIX)) {
            throw new SecurityException("Update URL is outside the approved Nova Next release channel");
        }
        if (!info.downloadUrl.endsWith(".apk")) {
            throw new SecurityException("Update URL is not an APK release asset");
        }
    }

    private void verifyApkIdentity(File apk, int expectedVersionCode) throws Exception {
        PackageManager manager = activity.getPackageManager();
        PackageInfo archive = manager.getPackageArchiveInfo(
                apk.getAbsolutePath(), PackageManager.GET_SIGNING_CERTIFICATES);
        if (archive == null) throw new SecurityException("Downloaded APK package metadata is unreadable");
        if (!activity.getPackageName().equals(archive.packageName)) {
            throw new SecurityException("Downloaded APK package name mismatch");
        }
        if (archive.getLongVersionCode() != expectedVersionCode) {
            throw new SecurityException("Downloaded APK version code mismatch");
        }

        PackageInfo installed = manager.getPackageInfo(
                activity.getPackageName(), PackageManager.GET_SIGNING_CERTIFICATES);
        Set<String> expectedSigners = signerDigests(installed);
        Set<String> archiveSigners = signerDigests(archive);
        if (expectedSigners.isEmpty() || !expectedSigners.equals(archiveSigners)) {
            throw new SecurityException("Downloaded APK signing identity mismatch");
        }
    }

    private static Set<String> signerDigests(PackageInfo info) throws Exception {
        Set<String> digests = new HashSet<>();
        if (info.signingInfo == null) return digests;
        Signature[] signatures = info.signingInfo.hasMultipleSigners()
                ? info.signingInfo.getApkContentsSigners()
                : info.signingInfo.getSigningCertificateHistory();
        if (signatures == null) return digests;
        for (Signature signature : signatures) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] result = digest.digest(signature.toByteArray());
            StringBuilder hex = new StringBuilder(64);
            for (byte value : result) hex.append(String.format(Locale.US, "%02x", value));
            digests.add(hex.toString());
        }
        return digests;
    }

    private void installVerifiedApk(File apk) {
        if (!activity.getPackageManager().canRequestPackageInstalls()) {
            listener.onStatus("Allow Nova Next to install updates. Installation resumes when you return.");
            Intent settings = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + activity.getPackageName()));
            activity.startActivity(settings);
            return;
        }
        launchInstaller(activity, apk);
        clearPending(activity);
    }

    static void resumePendingInstall(Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences(UPDATE_PREFS, Context.MODE_PRIVATE);
        int pendingCode = prefs.getInt(PENDING_CODE, 0);
        String path = prefs.getString(PENDING_PATH, "");
        String expected = prefs.getString(PENDING_SHA, "");
        if (pendingCode <= BuildConfig.VERSION_CODE) {
            clearPending(activity);
            return;
        }
        if (path == null || path.isBlank() || expected == null || !expected.matches("[0-9a-f]{64}")) {
            clearPending(activity);
            return;
        }
        if (!activity.getPackageManager().canRequestPackageInstalls()) return;
        File apk = new File(path);
        try {
            UpdateManager verifier = new UpdateManager(activity, new Listener() {
                @Override public void onStatus(String message) { }
                @Override public void onUpdateAvailable(UpdateInfo info) { }
                @Override public void onUpToDate() { }
                @Override public void onError(String message) { }
            });
            if (!apk.isFile() || !expected.equals(sha256(apk))) {
                if (apk.exists()) apk.delete();
                clearPending(activity);
                return;
            }
            verifier.verifyApkIdentity(apk, pendingCode);
            launchInstaller(activity, apk);
        } catch (Exception ignored) {
            clearPending(activity);
            return;
        }
        clearPending(activity);
    }

    private static void launchInstaller(Activity activity, File apk) {
        Uri uri = FileProvider.getUriForFile(activity,
                activity.getPackageName() + ".files", apk);
        Intent install = new Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(install);
    }

    private static void rememberVerifiedUpdate(Context context, File apk, String sha, int code) {
        context.getSharedPreferences(UPDATE_PREFS, Context.MODE_PRIVATE).edit()
                .putString(PENDING_PATH, apk.getAbsolutePath())
                .putString(PENDING_SHA, sha)
                .putInt(PENDING_CODE, code)
                .apply();
    }

    private static void clearObsoletePending(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(UPDATE_PREFS, Context.MODE_PRIVATE);
        if (prefs.getInt(PENDING_CODE, 0) <= BuildConfig.VERSION_CODE) clearPending(context);
    }

    private static void clearPending(Context context) {
        context.getSharedPreferences(UPDATE_PREFS, Context.MODE_PRIVATE).edit().clear().apply();
    }

    private static String readFully(InputStream input) throws Exception {
        StringBuilder builder = new StringBuilder();
        byte[] buffer = new byte[8 * 1024];
        int read;
        while ((read = input.read(buffer)) != -1) {
            builder.append(new String(buffer, 0, read, StandardCharsets.UTF_8));
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
