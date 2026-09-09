package com.buysloans.nova;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class NovaVisionActivity extends Activity {
    private static final int REQ_CAMERA_PERMISSION = 9110;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private Uri pendingCameraUri;
    private TextView result;
    private EditText hint;
    private Button camera, gallery, valuation;
    private JSONObject lastAssessment;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        buildUi();
    }

    private void buildUi() {
        int background = Color.rgb(2, 10, 22), surface = Color.rgb(7, 23, 42), outline = Color.rgb(38, 66, 101);
        int primary = Color.rgb(239, 244, 255), secondary = Color.rgb(169, 185, 211), accent = Color.rgb(82, 139, 255);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(background);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(24), dp(18), dp(32));
        scroll.addView(root);

        TextView brand = text("NOVA AI  •  VISION", 14, accent);
        brand.setLetterSpacing(.18f);
        brand.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(brand);
        TextView title = text("Device image intelligence", 24, primary);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setPadding(0, dp(14), 0, dp(4));
        root.addView(title);
        root.addView(text("Take one photo or choose up to six images. Nova combines evidence across angles, grades visible condition, identifies accessories and requests another angle only when useful. Catalogue and final pricing remain human-controlled.", 13, secondary));

        hint = new EditText(this);
        hint.setHint("Optional hint — e.g. ‘customer says iPhone 15 Pro 256GB’");
        hint.setHintTextColor(secondary);
        hint.setTextColor(primary);
        hint.setSingleLine(false);
        hint.setMinLines(2);
        hint.setPadding(dp(14), dp(12), dp(14), dp(12));
        hint.setBackground(rounded(surface, 16, outline));
        LinearLayout.LayoutParams hintLp = full();
        hintLp.topMargin = dp(18);
        root.addView(hint, hintLp);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        camera = button("Take photo", true, primary, accent, surface, outline);
        gallery = button("Choose up to 6", false, primary, accent, surface, outline);
        LinearLayout.LayoutParams left = new LinearLayout.LayoutParams(0, dp(52), 1f);
        left.setMarginEnd(dp(5));
        LinearLayout.LayoutParams right = new LinearLayout.LayoutParams(0, dp(52), 1f);
        right.setMarginStart(dp(5));
        row.addView(camera, left);
        row.addView(gallery, right);
        LinearLayout.LayoutParams rowLp = full();
        rowLp.topMargin = dp(12);
        root.addView(row, rowLp);

        result = text("Ready. For best results include front, back, model label and any visible damage.", 14, secondary);
        result.setPadding(dp(16), dp(16), dp(16), dp(16));
        result.setBackground(rounded(surface, 18, outline));
        LinearLayout.LayoutParams resultLp = full();
        resultLp.topMargin = dp(16);
        root.addView(result, resultLp);

        valuation = button("Research valuation", false, primary, accent, surface, outline);
        valuation.setEnabled(false);
        valuation.setOnClickListener(v -> researchValuation());
        LinearLayout.LayoutParams valuationLp = full();
        valuationLp.topMargin = dp(12);
        root.addView(valuation, valuationLp);
        TextView valuationBoundary = text("Pricing research is advisory. A/B/C guidance uses Morley’s existing 70% / 50% / 30% market-value factors. D/PARTS requires manual pricing and a human approves every final offer.", 12, secondary);
        valuationBoundary.setPadding(0, dp(8), 0, 0);
        root.addView(valuationBoundary);

        Button back = button("Back to Nova", false, primary, accent, surface, outline);
        back.setOnClickListener(v -> finish());
        LinearLayout.LayoutParams backLp = full();
        backLp.topMargin = dp(18);
        root.addView(back, backLp);

        camera.setOnClickListener(v -> beginCamera());
        gallery.setOnClickListener(v -> startActivityForResult(NovaAndroidOperator.imagePickerIntent(), NovaAndroidOperator.REQ_IMAGE));
        setContentView(scroll);
    }

    private void beginCamera() {
        if (android.os.Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQ_CAMERA_PERMISSION);
            return;
        }
        try {
            pendingCameraUri = NovaAndroidOperator.createCameraUri(this);
            startActivityForResult(NovaAndroidOperator.cameraIntent(pendingCameraUri), NovaAndroidOperator.REQ_CAMERA);
        } catch (Exception e) { showError(e.getMessage()); }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) beginCamera();
            else showError("Camera permission is required to take a photo. You can still choose existing images.");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) return;
        ArrayList<Uri> uris = new ArrayList<>();
        if (requestCode == NovaAndroidOperator.REQ_CAMERA && pendingCameraUri != null) uris.add(pendingCameraUri);
        else if (requestCode == NovaAndroidOperator.REQ_IMAGE && data != null) {
            ClipData clip = data.getClipData();
            if (clip != null) {
                int count = Math.min(clip.getItemCount(), NovaAndroidOperator.MAX_VISION_PHOTOS);
                for (int i = 0; i < count; i++) {
                    Uri uri = clip.getItemAt(i).getUri();
                    if (uri != null) uris.add(uri);
                }
            } else if (data.getData() != null) uris.add(data.getData());
        }
        if (!uris.isEmpty()) analyse(uris);
    }

    private void analyse(List<Uri> uris) {
        NovaApiClient api = NovaSessionBridge.api();
        if (api == null || !api.isSignedIn()) {
            showError("Your active Nova session is not available in this screen. Return to Nova and open Vision again.");
            return;
        }
        camera.setEnabled(false);
        gallery.setEnabled(false);
        valuation.setEnabled(false);
        lastAssessment = null;
        result.setTextColor(Color.rgb(169, 185, 211));
        result.setText("Analysing " + uris.size() + " photo" + (uris.size() == 1 ? "" : "s") + " with Nova Vision…");
        String hintText = hint.getText().toString().trim();
        worker.execute(() -> {
            try {
                JSONArray images = new JSONArray();
                for (Uri uri : uris) images.put(NovaAndroidOperator.imageDataUrl(this, uri));
                JSONObject payload = api.vision(images, hintText);
                JSONObject assessment = payload.optJSONObject("result");
                String formatted = NovaAndroidOperator.formatVision(payload);
                runOnUiThread(() -> {
                    lastAssessment = assessment;
                    result.setTextColor(Color.rgb(239, 244, 255));
                    result.setText(formatted);
                    camera.setEnabled(true);
                    gallery.setEnabled(true);
                    valuation.setEnabled(hasPricingIdentity(assessment));
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    showError(e.getMessage());
                    camera.setEnabled(true);
                    gallery.setEnabled(true);
                });
            }
        });
    }

    private void researchValuation() {
        NovaApiClient api = NovaSessionBridge.api();
        if (api == null || !api.isSignedIn() || !hasPricingIdentity(lastAssessment)) {
            showError("Nova needs a reliable device identity before valuation research.");
            return;
        }
        final JSONObject assessment = lastAssessment;
        final String query = pricingQuery(assessment);
        valuation.setEnabled(false);
        valuation.setText("Researching…");
        result.setText(result.getText() + "\n\nChecking current Australian market evidence…");
        worker.execute(() -> {
            try {
                JSONObject market = api.marketSearch(query);
                String formatted = formatValuation(assessment, market);
                String visionSummary = NovaAndroidOperator.formatVision(new JSONObject().put("result", assessment));
                runOnUiThread(() -> {
                    result.setTextColor(Color.rgb(239, 244, 255));
                    result.setText(visionSummary + "\n\n" + formatted);
                    valuation.setText("Research valuation");
                    valuation.setEnabled(true);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    showError("Pricing intelligence unavailable: " + (e.getMessage() == null ? "unknown error" : e.getMessage()));
                    valuation.setText("Research valuation");
                    valuation.setEnabled(true);
                });
            }
        });
    }

    private static boolean hasPricingIdentity(JSONObject assessment) {
        if (assessment == null) return false;
        return !assessment.optString("model_number").isBlank() || !assessment.optString("likely_model").isBlank();
    }

    private static String pricingQuery(JSONObject assessment) {
        StringBuilder q = new StringBuilder();
        for (String key : new String[]{"likely_brand", "likely_model", "model_number", "storage"}) {
            String value = assessment.optString(key).trim();
            if (!value.isEmpty()) {
                if (q.length() > 0) q.append(' ');
                q.append(value);
            }
        }
        return q.toString().replaceAll("\\s+", " ").trim();
    }

    private static String formatValuation(JSONObject assessment, JSONObject market) {
        ArrayList<Double> prices = new ArrayList<>();
        collectPrices(prices, market.optJSONObject("ebay"), "deliveredPrice", "price");
        collectPrices(prices, market.optJSONObject("gumtree"), "price");
        collectPrices(prices, market.optJSONObject("facebook"), "price");
        Collections.sort(prices);
        double median = median(prices);
        String grade = assessment.optString("condition_grade").trim().toUpperCase(Locale.ROOT);
        double factor = "A".equals(grade) ? .70 : "B".equals(grade) ? .50 : "C".equals(grade) ? .30 : 0;
        StringBuilder out = new StringBuilder("Australian pricing intelligence\n");
        if (median > 0) out.append("Used-market median: $").append(Math.round(median)).append(" from ").append(prices.size()).append(" retained listing").append(prices.size() == 1 ? "" : "s").append("\n");
        else out.append("Used-market median: no reliable marketplace median available\n");
        if (median > 0 && factor > 0) out.append("Suggested maximum buy: $").append(Math.round(median * factor)).append(" • Grade ").append(grade).append(" factor ").append(Math.round(factor * 100)).append("%\n");
        else if ("D".equals(grade) || "PARTS".equals(grade)) out.append("Suggested maximum buy: manual pricing required for ").append(grade).append(" condition\n");
        else out.append("Suggested maximum buy: confirm A, B or C condition first\n");
        out.append("Evidence policy: used-market results prioritise eBay AU, Gumtree and Facebook Marketplace. Retail results are reference-only.\n");
        out.append("Final price: human approval required.");
        return out.toString();
    }

    private static void collectPrices(List<Double> target, JSONObject group, String... keys) {
        if (group == null) return;
        JSONArray items = group.optJSONArray("items");
        if (items == null) return;
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.optJSONObject(i);
            if (item == null) continue;
            double value = 0;
            for (String key : keys) {
                value = item.optDouble(key, 0);
                if (value > 0) break;
            }
            if (Double.isFinite(value) && value > 0) target.add(value);
        }
    }

    private static double median(List<Double> values) {
        if (values.isEmpty()) return 0;
        int middle = values.size() / 2;
        return values.size() % 2 == 1 ? values.get(middle) : (values.get(middle - 1) + values.get(middle)) / 2.0;
    }

    private void showError(String message) {
        result.setTextColor(Color.rgb(255, 103, 122));
        result.setText(message == null || message.isBlank() ? "Nova Vision could not complete that assessment." : message);
    }

    private TextView text(String value, float size, int color) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(size);
        v.setTextColor(color);
        v.setLineSpacing(0, 1.08f);
        return v;
    }

    private Button button(String label, boolean primaryAction, int primary, int accent, int surface, int outline) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextColor(primary);
        b.setTextSize(14);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setGravity(Gravity.CENTER);
        b.setBackground(primaryAction ? rounded(Color.rgb(61, 100, 224), 16, accent) : rounded(surface, 16, outline));
        return b;
    }

    private GradientDrawable rounded(int fill, int radius, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(radius));
        if (stroke != Color.TRANSPARENT) d.setStroke(dp(1), stroke);
        return d;
    }

    private LinearLayout.LayoutParams full() { return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT); }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    @Override
    protected void onDestroy() {
        worker.shutdownNow();
        super.onDestroy();
    }
}
