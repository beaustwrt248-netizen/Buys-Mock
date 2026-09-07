package com.buysloans.nova;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Base64;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

final class NovaAndroidOperator {
    static final int REQ_CAMERA = 9101;
    static final int REQ_IMAGE = 9102;
    static final int REQ_NOTIFICATIONS = 9103;
    private static final String ALERT_CHANNEL = "nova_attention";
    private static final String ALERT_WORK = "nova-proactive-attention";
    private static final int MAX_IMAGE_BYTES = 6 * 1024 * 1024;

    private NovaAndroidOperator() {}

    static Uri createCameraUri(Activity activity) throws Exception {
        File dir = new File(activity.getCacheDir(), "vision");
        if (!dir.exists() && !dir.mkdirs()) throw new IllegalStateException("Could not prepare Nova camera cache.");
        File file = File.createTempFile("nova-device-", ".jpg", dir);
        return FileProvider.getUriForFile(activity, activity.getPackageName() + ".fileprovider", file);
    }

    static Intent cameraIntent(Uri destination) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, destination);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        return intent;
    }

    static Intent imagePickerIntent() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        return intent;
    }

    static String imageDataUrl(Context context, Uri uri) throws Exception {
        String mime = context.getContentResolver().getType(uri);
        if (mime == null || !(mime.equals("image/jpeg") || mime.equals("image/png") || mime.equals("image/webp"))) mime = "image/jpeg";
        byte[] bytes;
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            if (in == null) throw new IllegalArgumentException("The selected image could not be opened.");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[32 * 1024];
            int total = 0;
            for (int read; (read = in.read(buffer)) != -1;) {
                total += read;
                if (total > MAX_IMAGE_BYTES) throw new IllegalArgumentException("Use a photo smaller than 6 MB.");
                out.write(buffer, 0, read);
            }
            bytes = out.toByteArray();
        }
        return "data:" + mime + ";base64," + Base64.encodeToString(bytes, Base64.NO_WRAP);
    }

    static String formatVision(JSONObject payload) {
        JSONObject r = payload.optJSONObject("result");
        if (r == null) return "Nova Vision returned no structured result.";
        StringBuilder out = new StringBuilder("Nova Vision\n\n");
        append(out, "Likely device", join(r.optString("likely_brand"), r.optString("likely_model")));
        append(out, "Family", r.optString("likely_family"));
        append(out, "Model number", r.optString("model_number"));
        append(out, "Colour", r.optString("colour"));
        append(out, "Storage", r.optString("storage"));
        double confidence = r.optDouble("confidence", 0);
        out.append("Confidence: ").append(Math.round(confidence * 100)).append("%\n");
        appendArray(out, "Visible condition", r.optJSONArray("visible_condition"));
        appendArray(out, "Possible missing parts", r.optJSONArray("missing_parts"));
        appendArray(out, "Label identifiers", r.optJSONArray("label_identifiers"));
        appendArray(out, "Visible evidence", r.optJSONArray("evidence"));
        appendArray(out, "Uncertainties", r.optJSONArray("uncertainties"));
        out.append("\nPhoto privacy: not stored by Nova Vision; full serial/IMEI values are not returned.");
        return out.toString();
    }

    static void ensureNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) return;
        NotificationChannel channel = new NotificationChannel(ALERT_CHANNEL, "Nova attention alerts", NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("Actionable Morley alerts found by Nova AI.");
        manager.createNotificationChannel(channel);
    }

    static void scheduleBackgroundAlerts(Context context) {
        ensureNotificationChannel(context);
        Constraints constraints = new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(NovaAlertWorker.class, 15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build();
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(ALERT_WORK, ExistingPeriodicWorkPolicy.UPDATE, request);
    }

    static boolean notificationPermissionGranted(Activity activity) {
        return Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    static void requestNotificationPermission(Activity activity) {
        ensureNotificationChannel(activity);
        if (Build.VERSION.SDK_INT >= 33 && !notificationPermissionGranted(activity)) {
            activity.requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIFICATIONS);
        }
    }

    static void postAttentionNotification(Context context, String title, String body, int id) {
        ensureNotificationChannel(context);
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, ALERT_CHANNEL)
                .setSmallIcon(R.drawable.nova_official_app_icon)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) manager.notify(id, builder.build());
    }

    private static String join(String a, String b) {
        String x = a == null ? "" : a.trim();
        String y = b == null ? "" : b.trim();
        if (x.isEmpty()) return y;
        if (y.isEmpty()) return x;
        return x + " " + y;
    }

    private static void append(StringBuilder out, String label, String value) {
        if (value != null && !value.isBlank()) out.append(label).append(": ").append(value.trim()).append("\n");
    }

    private static void appendArray(StringBuilder out, String label, JSONArray values) {
        if (values == null || values.length() == 0) return;
        out.append(label).append(":\n");
        for (int i = 0; i < values.length(); i++) {
            String value = values.optString(i).trim();
            if (!value.isEmpty()) out.append("• ").append(value).append("\n");
        }
    }
}
