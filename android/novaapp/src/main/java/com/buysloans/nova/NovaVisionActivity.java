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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class NovaVisionActivity extends Activity {
    private static final int REQ_CAMERA_PERMISSION = 9110;
    private static final Pattern STORAGE_VALUE = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(TB|GB)", Pattern.CASE_INSENSITIVE);
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final ArrayList<Uri> sessionUris = new ArrayList<>();
    private Uri pendingCameraUri;
    private TextView result;
    private EditText hint;
    private Button camera, gallery, analyseSession, clearSession, valuation;
    private JSONObject lastAssessment;
    private JSONArray lastCatalogueMatches = new JSONArray();

    private static final class StorageCheck {
        final boolean known;
        final boolean conflict;
        final String detail;
        StorageCheck(boolean known, boolean conflict, String detail) {
            this.known = known;
            this.conflict = conflict;
            this.detail = detail;
        }
    }

    private static final class IdentityCheck {
        final boolean verified;
        final String detail;
        IdentityCheck(boolean verified, String detail) {
            this.verified = verified;
            this.detail = detail;
        }
    }

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
        root.addView(text("Build one evidence session with up to six camera or gallery photos. Nova combines every retained angle, checks active Morley catalogue matches, grades visible condition, requests useful follow-up angles and can research current Australian pricing. Catalogue and final pricing remain human-controlled.", 13, secondary));

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

        LinearLayout captureRow = new LinearLayout(this);
        captureRow.setOrientation(LinearLayout.HORIZONTAL);
        camera = button("Take photo", true, primary, accent, surface, outline);
        gallery = button("Choose up to 6", false, primary, accent, surface, outline);
        LinearLayout.LayoutParams captureLeft = new LinearLayout.LayoutParams(0, dp(52), 1f);
        captureLeft.setMarginEnd(dp(5));
        LinearLayout.LayoutParams captureRight = new LinearLayout.LayoutParams(0, dp(52), 1f);
        captureRight.setMarginStart(dp(5));
        captureRow.addView(camera, captureLeft);
        captureRow.addView(gallery, captureRight);
        LinearLayout.LayoutParams captureLp = full();
        captureLp.topMargin = dp(12);
        root.addView(captureRow, captureLp);

        LinearLayout sessionRow = new LinearLayout(this);
        sessionRow.setOrientation(LinearLayout.HORIZONTAL);
        analyseSession = button("Analyse photos", true, primary, accent, surface, outline);
        clearSession = button("Clear photos", false, primary, accent, surface, outline);
        LinearLayout.LayoutParams sessionLeft = new LinearLayout.LayoutParams(0, dp(52), 1f);
        sessionLeft.setMarginEnd(dp(5));
        LinearLayout.LayoutParams sessionRight = new LinearLayout.LayoutParams(0, dp(52), 1f);
        sessionRight.setMarginStart(dp(5));
        sessionRow.addView(analyseSession, sessionLeft);
        sessionRow.addView(clearSession, sessionRight);
        LinearLayout.LayoutParams sessionLp = full();
        sessionLp.topMargin = dp(8);
        root.addView(sessionRow, sessionLp);

        result = text("Ready. Add front, back, model-label or damage photos, then analyse the retained evidence together.", 14, secondary);
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
        TextView valuationBoundary = text("Pricing research is advisory. Market evidence can be researched from a plausible visual identity, but A/B/C maximum-buy guidance is withheld until Nova has a strong live-catalogue identity match. Storage conflicts block valuation research. D/PARTS requires manual pricing and a human approves every final offer.", 12, secondary);
        valuationBoundary.setPadding(0, dp(8), 0, 0);
        root.addView(valuationBoundary);

        Button back = button("Back to Nova", false, primary, accent, surface, outline);
        back.setOnClickListener(v -> finish());
        LinearLayout.LayoutParams backLp = full();
        backLp.topMargin = dp(18);
        root.addView(back, backLp);

        camera.setOnClickListener(v -> beginCamera());
        gallery.setOnClickListener(v -> startActivityForResult(NovaAndroidOperator.imagePickerIntent(), NovaAndroidOperator.REQ_IMAGE));
        analyseSession.setOnClickListener(v -> analyseSession());
        clearSession.setOnClickListener(v -> clearEvidenceSession());
        updateSessionControls();
        setContentView(scroll);
    }

    private void beginCamera() {
        if (sessionUris.size() >= NovaAndroidOperator.MAX_VISION_PHOTOS) {
            showError("This Vision assessment already has six photos. Analyse or clear the current evidence before adding more.");
            return;
        }
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
        if (requestCode == NovaAndroidOperator.REQ_CAMERA && pendingCameraUri != null) {
            if (sessionUris.size() < NovaAndroidOperator.MAX_VISION_PHOTOS) sessionUris.add(pendingCameraUri);
            pendingCameraUri = null;
            evidenceChanged();
            return;
        }
        if (requestCode == NovaAndroidOperator.REQ_IMAGE && data != null) {
            ArrayList<Uri> selected = new ArrayList<>();
            int remaining = NovaAndroidOperator.MAX_VISION_PHOTOS - sessionUris.size();
            if (remaining <= 0) {
                showError("This Vision assessment already has six photos. Analyse or clear the current evidence before adding more.");
                return;
            }
            ClipData clip = data.getClipData();
            if (clip != null) {
                int count = Math.min(clip.getItemCount(), remaining);
                for (int i = 0; i < count; i++) {
                    Uri uri = clip.getItemAt(i).getUri();
                    if (uri != null && !sessionUris.contains(uri) && !selected.contains(uri)) selected.add(uri);
                }
            } else if (data.getData() != null && !sessionUris.contains(data.getData())) selected.add(data.getData());
            if (!selected.isEmpty()) {
                sessionUris.addAll(selected);
                evidenceChanged();
            }
        }
    }

    private void evidenceChanged() {
        lastAssessment = null;
        lastCatalogueMatches = new JSONArray();
        valuation.setEnabled(false);
        updateSessionControls();
        result.setTextColor(Color.rgb(169, 185, 211));
        result.setText(sessionUris.size() + " photo" + (sessionUris.size() == 1 ? "" : "s") + " retained. Add another useful angle or analyse the full evidence set now.");
    }

    private void clearEvidenceSession() {
        sessionUris.clear();
        pendingCameraUri = null;
        lastAssessment = null;
        lastCatalogueMatches = new JSONArray();
        valuation.setEnabled(false);
        updateSessionControls();
        result.setTextColor(Color.rgb(169, 185, 211));
        result.setText("Evidence session cleared. Add new device photos to begin another assessment.");
    }

    private void updateSessionControls() {
        int count = sessionUris.size();
        if (camera != null) {
            camera.setEnabled(count < NovaAndroidOperator.MAX_VISION_PHOTOS);
            camera.setText(count == 0 ? "Take photo" : "Add photo (" + count + "/" + NovaAndroidOperator.MAX_VISION_PHOTOS + ")");
        }
        if (gallery != null) gallery.setEnabled(count < NovaAndroidOperator.MAX_VISION_PHOTOS);
        if (analyseSession != null) analyseSession.setEnabled(count > 0);
        if (clearSession != null) clearSession.setEnabled(count > 0);
    }

    private void setCaptureControlsEnabled(boolean enabled) {
        boolean room = sessionUris.size() < NovaAndroidOperator.MAX_VISION_PHOTOS;
        camera.setEnabled(enabled && room);
        gallery.setEnabled(enabled && room);
        analyseSession.setEnabled(enabled && !sessionUris.isEmpty());
        clearSession.setEnabled(enabled && !sessionUris.isEmpty());
    }

    private void analyseSession() {
        if (sessionUris.isEmpty()) {
            showError("Add at least one device photo before running Nova Vision.");
            return;
        }
        analyse(new ArrayList<>(sessionUris));
    }

    private void analyse(List<Uri> uris) {
        NovaApiClient api = NovaSessionBridge.api();
        if (api == null || !api.isSignedIn()) {
            showError("Your active Nova session is not available in this screen. Return to Nova and open Vision again.");
            return;
        }
        setCaptureControlsEnabled(false);
        valuation.setEnabled(false);
        lastAssessment = null;
        lastCatalogueMatches = new JSONArray();
        result.setTextColor(Color.rgb(169, 185, 211));
        result.setText("Analysing " + uris.size() + " retained photo" + (uris.size() == 1 ? "" : "s") + " with Nova Vision…");
        String hintText = hint.getText().toString().trim();
        worker.execute(() -> {
            try {
                JSONArray images = new JSONArray();
                for (Uri uri : uris) images.put(NovaAndroidOperator.imageDataUrl(this, uri));
                JSONObject payload = api.vision(images, hintText);
                JSONObject assessment = payload.optJSONObject("result");
                JSONArray matches = assessment == null ? new JSONArray() : api.catalogueMatches(visualIdentityQuery(assessment));
                StorageCheck storageCheck = checkStorage(assessment, matches);
                IdentityCheck identityCheck = checkIdentity(assessment, matches);
                String formatted = NovaAndroidOperator.formatVision(payload) + "\n\n" + formatCatalogueMatches(matches) + "\n" + storageCheck.detail + "\n" + identityCheck.detail;
                runOnUiThread(() -> {
                    lastAssessment = assessment;
                    lastCatalogueMatches = matches;
                    result.setTextColor(Color.rgb(239, 244, 255));
                    result.setText(formatted);
                    setCaptureControlsEnabled(true);
                    updateSessionControls();
                    valuation.setEnabled(hasPricingIdentity(assessment) && !storageCheck.conflict);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    showError(e.getMessage());
                    setCaptureControlsEnabled(true);
                    updateSessionControls();
                });
            }
        });
    }

    private void researchValuation() {
        NovaApiClient api = NovaSessionBridge.api();
        StorageCheck storageCheck = checkStorage(lastAssessment, lastCatalogueMatches);
        if (storageCheck.conflict) {
            showError("Valuation blocked: " + storageCheck.detail + " Verify the storage variant or add clearer evidence, then analyse again.");
            return;
        }
        if (api == null || !api.isSignedIn() || !hasPricingIdentity(lastAssessment)) {
            showError("Nova needs a reliable device identity before valuation research.");
            return;
        }
        final JSONObject assessment = lastAssessment;
        final JSONArray matches = lastCatalogueMatches;
        final IdentityCheck identityCheck = checkIdentity(assessment, matches);
        final String query = pricingQuery(assessment, matches);
        valuation.setEnabled(false);
        valuation.setText("Researching…");
        result.setText(result.getText() + "\n\nChecking current Australian market evidence…");
        worker.execute(() -> {
            try {
                JSONObject market = api.marketSearch(query);
                String formatted = formatValuation(assessment, market, identityCheck.verified);
                String visionSummary = NovaAndroidOperator.formatVision(new JSONObject().put("result", assessment));
                String catalogueSummary = formatCatalogueMatches(matches);
                runOnUiThread(() -> {
                    result.setTextColor(Color.rgb(239, 244, 255));
                    result.setText(visionSummary + "\n\n" + catalogueSummary + "\n" + storageCheck.detail + "\n" + identityCheck.detail + "\n\n" + formatted);
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

    private static String visualIdentityQuery(JSONObject assessment) {
        String modelNumber = assessment.optString("model_number").trim();
        if (!modelNumber.isEmpty()) return modelNumber;
        return pricingQuery(assessment, new JSONArray());
    }

    private static String pricingQuery(JSONObject assessment, JSONArray matches) {
        StringBuilder q = new StringBuilder();
        JSONObject match = matches == null ? null : matches.optJSONObject(0);
        if (match != null) {
            for (String key : new String[]{"brand", "model_name", "model_number"}) {
                String value = match.optString(key).trim();
                if (!value.isEmpty()) {
                    if (q.length() > 0) q.append(' ');
                    q.append(value);
                }
            }
        } else {
            for (String key : new String[]{"likely_brand", "likely_model", "model_number"}) {
                String value = assessment.optString(key).trim();
                if (!value.isEmpty()) {
                    if (q.length() > 0) q.append(' ');
                    q.append(value);
                }
            }
        }
        String storage = assessment.optString("storage").trim();
        if (!storage.isEmpty()) {
            if (q.length() > 0) q.append(' ');
            q.append(storage);
        }
        return q.toString().replaceAll("\\s+", " ").trim();
    }

    private static IdentityCheck checkIdentity(JSONObject assessment, JSONArray matches) {
        if (assessment == null) return new IdentityCheck(false, "Identity verification: no visual identity is available.");
        String visualModelNumber = normaliseIdentity(assessment.optString("model_number"));
        if (!visualModelNumber.isEmpty() && matches != null) {
            for (int i = 0; i < matches.length(); i++) {
                JSONObject row = matches.optJSONObject(i);
                if (row == null || !visualModelNumber.equals(normaliseIdentity(row.optString("model_number")))) continue;
                if (storageCompatibleWithMatch(assessment, row)) return new IdentityCheck(true, "Identity verification: exact model number matched the live catalogue.");
                return new IdentityCheck(false, "Identity verification: model number matched, but the visible storage conflicts with that catalogue variant.");
            }
        }
        String visualModel = normaliseIdentity(assessment.optString("likely_model"));
        String visualBrand = normaliseIdentity(assessment.optString("likely_brand"));
        double confidence = assessment.optDouble("confidence", 0);
        if (!visualModel.isEmpty() && confidence >= .90 && matches != null) {
            JSONObject candidate = null;
            int count = 0;
            for (int i = 0; i < matches.length(); i++) {
                JSONObject row = matches.optJSONObject(i);
                if (row == null || !visualModel.equals(normaliseIdentity(row.optString("model_name")))) continue;
                String rowBrand = normaliseIdentity(row.optString("brand"));
                if (!visualBrand.isEmpty() && !rowBrand.isEmpty() && !visualBrand.equals(rowBrand)) continue;
                candidate = row;
                count++;
            }
            if (count == 1 && candidate != null) {
                if (storageCompatibleWithMatch(assessment, candidate)) return new IdentityCheck(true, "Identity verification: one high-confidence model-name match was found in the live catalogue.");
                return new IdentityCheck(false, "Identity verification: high-confidence model matched, but the visible storage conflicts with that catalogue variant.");
            }
        }
        return new IdentityCheck(false, matches == null || matches.length() == 0 ? "Identity verification: no strong live-catalogue match was found; automatic max-buy guidance will be withheld." : "Identity verification: catalogue identity remains ambiguous; automatic max-buy guidance will be withheld.");
    }

    private static String normaliseIdentity(String raw) {
        return raw == null ? "" : raw.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "");
    }

    private static boolean storageCompatibleWithMatch(JSONObject assessment, JSONObject row) {
        Double visualGb = storageGb(assessment == null ? null : assessment.optString("storage"));
        JSONArray options = row == null ? null : row.optJSONArray("storage_options");
        if (visualGb == null || options == null || options.length() == 0) return true;
        boolean parsedAny = false;
        for (int i = 0; i < options.length(); i++) {
            Double optionGb = storageGb(options.optString(i));
            if (optionGb == null) continue;
            parsedAny = true;
            if (Math.abs(optionGb - visualGb) < .5) return true;
        }
        return !parsedAny;
    }

    private static StorageCheck checkStorage(JSONObject assessment, JSONArray matches) {
        if (assessment == null) return new StorageCheck(false, false, "Storage verification: no visual storage evidence yet.");
        String visual = assessment.optString("storage").trim();
        Double visualGb = storageGb(visual);
        if (visual.isEmpty() || visualGb == null) return new StorageCheck(false, false, "Storage verification: storage is unknown or not reliably visible.");
        boolean explicitOptions = false;
        ArrayList<String> options = new ArrayList<>();
        if (matches != null) {
            for (int i = 0; i < matches.length(); i++) {
                JSONObject row = matches.optJSONObject(i);
                if (row == null) continue;
                JSONArray storage = row.optJSONArray("storage_options");
                if (storage == null || storage.length() == 0) continue;
                explicitOptions = true;
                for (int j = 0; j < storage.length(); j++) {
                    String option = storage.optString(j).trim();
                    if (!option.isEmpty() && !options.contains(option)) options.add(option);
                    Double optionGb = storageGb(option);
                    if (optionGb != null && Math.abs(optionGb - visualGb) < 0.5) {
                        return new StorageCheck(true, false, "Storage verification: " + visual + " is supported by the matched catalogue model.");
                    }
                }
            }
        }
        if (!explicitOptions) return new StorageCheck(false, false, "Storage verification: catalogue storage options are unavailable, so no conflict is inferred.");
        return new StorageCheck(true, true, "Storage conflict: Vision inferred " + visual + " but matched catalogue options are " + String.join(", ", options) + ".");
    }

    private static Double storageGb(String raw) {
        if (raw == null) return null;
        Matcher matcher = STORAGE_VALUE.matcher(raw);
        if (!matcher.find()) return null;
        try {
            double value = Double.parseDouble(matcher.group(1));
            return "TB".equalsIgnoreCase(matcher.group(2)) ? value * 1024d : value;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String formatCatalogueMatches(JSONArray matches) {
        StringBuilder out = new StringBuilder("Morley catalogue verification\n");
        if (matches == null || matches.length() == 0) return out.append("No active catalogue match found for the current visual identity.").toString();
        int count = Math.min(matches.length(), 3);
        out.append(matches.length()).append(" possible active match").append(matches.length() == 1 ? "" : "es").append("\n");
        for (int i = 0; i < count; i++) {
            JSONObject row = matches.optJSONObject(i);
            if (row == null) continue;
            String name = (row.optString("brand") + " " + row.optString("model_name")).trim();
            out.append("• ").append(name.isEmpty() ? "Catalogue record" : name);
            String model = row.optString("model_number").trim();
            if (!model.isEmpty()) out.append(" • ").append(model);
            JSONArray storage = row.optJSONArray("storage_options");
            if (storage != null && storage.length() > 0) out.append(" • storage options available");
            out.append("\n");
        }
        out.append("Catalogue matches are evidence only; no record is modified from Vision.");
        return out.toString();
    }

    private static String formatValuation(JSONObject assessment, JSONObject market, boolean verifiedIdentity) {
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
        if (median > 0 && factor > 0 && verifiedIdentity) out.append("Suggested maximum buy: $").append(Math.round(median * factor)).append(" • Grade ").append(grade).append(" factor ").append(Math.round(factor * 100)).append("%\n");
        else if (median > 0 && factor > 0) out.append("Suggested maximum buy: withheld until catalogue identity is verified\n");
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
