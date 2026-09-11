package com.buysloans.nova;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;

public final class NovaDamageReviewActivity extends Activity {
    private final ArrayList<DamagePhotoView> photoViews = new ArrayList<>();

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        NovaVisionReviewBridge.Snapshot snapshot = NovaVisionReviewBridge.snapshot();
        if (snapshot == null) {
            TextView unavailable = text("No highlighted Vision evidence is available. Return to Nova Vision, analyse the photos, then open damage review again.", 15, Color.rgb(239, 244, 255));
            unavailable.setPadding(dp(22), dp(28), dp(22), dp(28));
            unavailable.setBackgroundColor(Color.rgb(2, 10, 22));
            setContentView(unavailable);
            return;
        }
        build(snapshot);
    }

    private void build(NovaVisionReviewBridge.Snapshot snapshot) {
        int background = Color.rgb(2, 10, 22), surface = Color.rgb(7, 23, 42), primary = Color.rgb(239, 244, 255), secondary = Color.rgb(169, 185, 211), accent = Color.rgb(82, 139, 255);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(background);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(24), dp(18), dp(32));
        scroll.addView(root);

        TextView brand = text("NOVA AI  •  DAMAGE REVIEW", 13, accent);
        brand.setLetterSpacing(.15f);
        root.addView(brand);
        TextView title = text("Highlighted photo evidence", 24, primary);
        title.setPadding(0, dp(12), 0, dp(5));
        root.addView(title);

        JSONArray allRegions = snapshot.assessment.optJSONArray("damage_regions");
        int total = allRegions == null ? 0 : allRegions.length();
        TextView helper = text(total + " visible damage region" + (total == 1 ? "" : "s") + " localized by Nova Vision. Circles are advisory evidence only—staff must physically inspect every device before grading or pricing.", 13, secondary);
        helper.setPadding(0, 0, 0, dp(14));
        root.addView(helper);

        for (int i = 0; i < snapshot.photos.size(); i++) {
            int photoIndex = i + 1;
            JSONArray regions = regionsForPhoto(allRegions, photoIndex);
            TextView heading = text("Photo " + photoIndex + (regions.length() == 0 ? " • no localized damage" : " • " + regions.length() + " highlighted"), 14, primary);
            heading.setPadding(0, dp(10), 0, dp(6));
            root.addView(heading);

            DamagePhotoView image = new DamagePhotoView(snapshot.photos.get(i), regions);
            photoViews.add(image);
            LinearLayout.LayoutParams imageLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(360));
            root.addView(image, imageLp);

            if (regions.length() > 0) {
                TextView legend = text(regionLegend(regions), 12, secondary);
                legend.setPadding(dp(4), dp(7), dp(4), dp(8));
                legend.setBackgroundColor(surface);
                root.addView(legend);
            }
        }

        TextView boundary = text("Nova only circles regions it can localize from visible pixels. Missing circles do not prove a device is damage-free, and hidden functionality remains covered by the manual functionality gate.", 12, secondary);
        boundary.setPadding(0, dp(16), 0, dp(12));
        root.addView(boundary);

        Button back = new Button(this);
        back.setText("Back to Vision");
        back.setAllCaps(false);
        back.setTextColor(primary);
        back.setBackgroundColor(Color.rgb(61, 100, 224));
        back.setOnClickListener(v -> finish());
        root.addView(back, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));
        setContentView(scroll);
    }

    private static JSONArray regionsForPhoto(JSONArray all, int photoIndex) {
        JSONArray out = new JSONArray();
        if (all == null) return out;
        for (int i = 0; i < all.length(); i++) {
            JSONObject region = all.optJSONObject(i);
            if (region != null && region.optInt("photo_index") == photoIndex) out.put(region);
        }
        return out;
    }

    private static String regionLegend(JSONArray regions) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < regions.length(); i++) {
            JSONObject region = regions.optJSONObject(i);
            if (region == null) continue;
            if (out.length() > 0) out.append('\n');
            out.append(i + 1).append(". ").append(region.optString("label", "Visible damage"));
            String severity = region.optString("severity", "unknown");
            double confidence = region.optDouble("confidence", 0);
            out.append(" • ").append(severity);
            if (confidence > 0) out.append(" • ").append(Math.round(confidence * 100)).append("%");
        }
        return out.toString();
    }

    private TextView text(String value, float size, int color) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(size);
        v.setTextColor(color);
        v.setLineSpacing(0, 1.08f);
        return v;
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    @Override
    protected void onDestroy() {
        for (DamagePhotoView view : photoViews) view.release();
        photoViews.clear();
        super.onDestroy();
    }

    private final class DamagePhotoView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final JSONArray regions;
        private Bitmap bitmap;

        DamagePhotoView(Uri uri, JSONArray regions) {
            super(NovaDamageReviewActivity.this);
            this.regions = regions == null ? new JSONArray() : regions;
            setBackgroundColor(Color.rgb(7, 23, 42));
            bitmap = decode(uri, 1800);
        }

        void release() {
            if (bitmap != null && !bitmap.isRecycled()) bitmap.recycle();
            bitmap = null;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (bitmap == null || bitmap.isRecycled()) {
                paint.setColor(Color.rgb(169, 185, 211));
                paint.setTextSize(dp(14));
                canvas.drawText("Photo unavailable", dp(16), dp(30), paint);
                return;
            }
            float vw = getWidth(), vh = getHeight();
            float scale = Math.min(vw / bitmap.getWidth(), vh / bitmap.getHeight());
            float dw = bitmap.getWidth() * scale, dh = bitmap.getHeight() * scale;
            float left = (vw - dw) / 2f, top = (vh - dh) / 2f;
            RectF dst = new RectF(left, top, left + dw, top + dh);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.WHITE);
            canvas.drawBitmap(bitmap, null, dst, paint);

            for (int i = 0; i < regions.length(); i++) {
                JSONObject region = regions.optJSONObject(i);
                if (region == null) continue;
                float x = left + (float) region.optDouble("center_x", .5) * dw;
                float y = top + (float) region.optDouble("center_y", .5) * dh;
                float radius = (float) region.optDouble("radius", .08) * Math.min(dw, dh);
                radius = Math.max(dp(14), radius);
                int circle = severityColor(region.optString("severity", "unknown"));
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(dp(3));
                paint.setColor(circle);
                canvas.drawCircle(x, y, radius, paint);

                String number = String.valueOf(i + 1);
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(circle);
                canvas.drawCircle(x + radius * .70f, y - radius * .70f, dp(11), paint);
                paint.setColor(Color.WHITE);
                paint.setTextSize(dp(11));
                paint.setTextAlign(Paint.Align.CENTER);
                Paint.FontMetrics fm = paint.getFontMetrics();
                canvas.drawText(number, x + radius * .70f, y - radius * .70f - (fm.ascent + fm.descent) / 2f, paint);
                paint.setTextAlign(Paint.Align.LEFT);
            }
        }

        private int severityColor(String severity) {
            if ("severe".equalsIgnoreCase(severity)) return Color.rgb(255, 80, 105);
            if ("moderate".equalsIgnoreCase(severity)) return Color.rgb(255, 169, 70);
            return Color.rgb(255, 217, 92);
        }

        private Bitmap decode(Uri uri, int maxEdge) {
            try {
                BitmapFactory.Options bounds = new BitmapFactory.Options();
                bounds.inJustDecodeBounds = true;
                try (InputStream in = getContentResolver().openInputStream(uri)) { BitmapFactory.decodeStream(in, null, bounds); }
                int sample = 1;
                int edge = Math.max(bounds.outWidth, bounds.outHeight);
                while (edge / sample > maxEdge * 2) sample *= 2;
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = Math.max(1, sample);
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                try (InputStream in = getContentResolver().openInputStream(uri)) { return BitmapFactory.decodeStream(in, null, options); }
            } catch (Exception ignored) {
                return null;
            }
        }
    }
}
