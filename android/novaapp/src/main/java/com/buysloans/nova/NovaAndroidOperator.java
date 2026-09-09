package com.buysloans.nova;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Size;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;

final class NovaAndroidOperator {
    static final int REQ_CAMERA = 9101;
    static final int REQ_IMAGE = 9102;
    static final int REQ_NOTIFICATIONS = 9103;
    static final int MAX_VISION_PHOTOS = 6;
    private static final String ALERT_CHANNEL = "nova_attention";
    private static final int ALERT_JOB_ID = 8201;
    private static final int MAX_IMAGE_BYTES = 6 * 1024 * 1024;
    private static final int VISION_LONG_EDGE = 2560;
    private static final int VISION_FALLBACK_EDGE = 2048;
    private static final int TARGET_IMAGE_BYTES = 2_250_000;

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
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        return intent;
    }

    static String imageDataUrl(Context context, Uri uri) throws Exception {
        Bitmap bitmap = decodeVisionBitmap(context, uri, VISION_LONG_EDGE);
        try {
            byte[] bytes = encodeVisionJpeg(bitmap);
            if (bytes.length > MAX_IMAGE_BYTES) throw new IllegalArgumentException("Use a photo smaller than 6 MB after preprocessing.");
            return "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP);
        } finally {
            bitmap.recycle();
        }
    }

    private static Bitmap decodeVisionBitmap(Context context, Uri uri, int longEdgeLimit) throws Exception {
        ImageDecoder.Source source = ImageDecoder.createSource(context.getContentResolver(), uri);
        return ImageDecoder.decodeBitmap(source, (decoder, info, src) -> {
            decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
            Size size = info.getSize();
            int width = size.getWidth();
            int height = size.getHeight();
            int longEdge = Math.max(width, height);
            if (longEdge > longEdgeLimit) {
                double scale = (double) longEdgeLimit / (double) longEdge;
                int targetWidth = Math.max(1, (int) Math.round(width * scale));
                int targetHeight = Math.max(1, (int) Math.round(height * scale));
                decoder.setTargetSize(targetWidth, targetHeight);
            }
        });
    }

    private static byte[] encodeVisionJpeg(Bitmap bitmap) {
        byte[] encoded = compress(bitmap, new int[]{88, 80, 72, 64, 56, 48});
        if (encoded.length <= TARGET_IMAGE_BYTES) return encoded;
        int longEdge = Math.max(bitmap.getWidth(), bitmap.getHeight());
        if (longEdge > VISION_FALLBACK_EDGE) {
            double scale = (double) VISION_FALLBACK_EDGE / (double) longEdge;
            int width = Math.max(1, (int) Math.round(bitmap.getWidth() * scale));
            int height = Math.max(1, (int) Math.round(bitmap.getHeight() * scale));
            Bitmap smaller = Bitmap.createScaledBitmap(bitmap, width, height, true);
            try { encoded = compress(smaller, new int[]{80, 72, 64, 56, 48, 40}); }
            finally { if (smaller != bitmap) smaller.recycle(); }
        }
        if (encoded.length > TARGET_IMAGE_BYTES) throw new IllegalArgumentException("This photo could not be safely compressed for Nova Vision. Retake it closer to the device or choose a smaller image.");
        return encoded;
    }

    private static byte[] compress(Bitmap bitmap, int[] qualities) {
        byte[] best = new byte[0];
        for (int quality : qualities) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)) throw new IllegalArgumentException("The selected image could not be encoded for Nova Vision.");
            best = out.toByteArray();
            if (best.length <= TARGET_IMAGE_BYTES) break;
        }
        return best;
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
        append(out, "Condition grade", r.optString("condition_grade"));
        append(out, "Condition summary", r.optString("condition_summary"));
        double confidence = r.optDouble("confidence", 0);
        out.append("Confidence: ").append(Math.round(confidence * 100)).append("%\n");
        appendArray(out, "Visible condition", r.optJSONArray("visible_condition"));
        appendArray(out, "Damage flags", r.optJSONArray("damage_flags"));
        appendArray(out, "Accessories present", r.optJSONArray("accessories_present"));
        appendArray(out, "Possible missing parts", r.optJSONArray("missing_parts"));
        appendArray(out, "Next useful photos", r.optJSONArray("next_photos"));
        appendArray(out, "Label identifiers", r.optJSONArray("label_identifiers"));
        appendArray(out, "Visible evidence", r.optJSONArray("evidence"));
        appendEvidenceByPhoto(out, r.optJSONArray("evidence_by_photo"));
        appendArray(out, "Uncertainties", r.optJSONArray("uncertainties"));
        out.append("\nPhoto privacy: not stored by Nova Vision; photos are re-encoded before upload where supported; full serial/IMEI values are not returned.");
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
        JobScheduler scheduler = context.getSystemService(JobScheduler.class);
        if (scheduler == null) return;
        JobInfo job = new JobInfo.Builder(ALERT_JOB_ID, new ComponentName(context, NovaAlertWorker.class)).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setPersisted(true).setPeriodic(15 * 60 * 1000L).build();
        scheduler.schedule(job);
    }

    static boolean notificationPermissionGranted(Activity activity) {
        return Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    static void requestNotificationPermission(Activity activity) {
        ensureNotificationChannel(activity);
        if (Build.VERSION.SDK_INT >= 33 && !notificationPermissionGranted(activity)) activity.requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIFICATIONS);
    }

    static void postAttentionNotification(Context context, String title, String body, int id) {
        ensureNotificationChannel(context);
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, ALERT_CHANNEL).setSmallIcon(R.drawable.nova_official_app_icon).setContentTitle(title).setContentText(body).setStyle(new NotificationCompat.BigTextStyle().bigText(body)).setPriority(NotificationCompat.PRIORITY_DEFAULT).setAutoCancel(true);
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) manager.notify(id, builder.build());
    }

    private static String join(String a, String b) { String x=a==null?"":a.trim(), y=b==null?"":b.trim(); if(x.isEmpty())return y; if(y.isEmpty())return x; return x+" "+y; }
    private static void append(StringBuilder out,String label,String value){if(value!=null&&!value.isBlank())out.append(label).append(": ").append(value.trim()).append("\n");}
    private static void appendArray(StringBuilder out,String label,JSONArray values){if(values==null||values.length()==0)return;out.append(label).append(":\n");for(int i=0;i<values.length();i++){String value=values.optString(i).trim();if(!value.isEmpty())out.append("• ").append(value).append("\n");}}
    private static void appendEvidenceByPhoto(StringBuilder out,JSONArray groups){if(groups==null||groups.length()==0)return;out.append("Evidence by photo:\n");for(int i=0;i<groups.length();i++){JSONObject group=groups.optJSONObject(i);if(group==null)continue;int photo=group.optInt("photo_index",0);JSONArray evidence=group.optJSONArray("evidence");if(photo<1||photo>MAX_VISION_PHOTOS||evidence==null||evidence.length()==0)continue;for(int j=0;j<evidence.length();j++){String value=evidence.optString(j).trim();if(!value.isEmpty())out.append("• Photo ").append(photo).append(": ").append(value).append("\n");}}}
}
