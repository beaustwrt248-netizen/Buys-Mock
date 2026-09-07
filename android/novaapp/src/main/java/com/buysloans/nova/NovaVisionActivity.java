package com.buysloans.nova;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class NovaVisionActivity extends Activity {
    private static final int REQ_CAMERA_PERMISSION = 9110;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private Uri pendingCameraUri;
    private TextView result;
    private EditText hint;
    private Button camera, gallery;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        buildUi();
    }

    private void buildUi() {
        int background = Color.rgb(2, 10, 22);
        int surface = Color.rgb(7, 23, 42);
        int outline = Color.rgb(38, 66, 101);
        int primary = Color.rgb(239, 244, 255);
        int secondary = Color.rgb(169, 185, 211);
        int accent = Color.rgb(82, 139, 255);

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
        TextView sub = text("Take or choose a device photo. Nova separates visible evidence from uncertain inference and never applies catalogue changes automatically.", 13, secondary);
        root.addView(sub);

        hint = new EditText(this);
        hint.setHint("Optional hint — e.g. ‘check the rear model label’");
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
        gallery = button("Choose photo", false, primary, accent, surface, outline);
        LinearLayout.LayoutParams left = new LinearLayout.LayoutParams(0, dp(52), 1f);
        left.setMarginEnd(dp(5));
        LinearLayout.LayoutParams right = new LinearLayout.LayoutParams(0, dp(52), 1f);
        right.setMarginStart(dp(5));
        row.addView(camera, left);
        row.addView(gallery, right);
        LinearLayout.LayoutParams rowLp = full();
        rowLp.topMargin = dp(12);
        root.addView(row, rowLp);

        result = text("Ready. Use a clear photo of the device, label or visible damage.", 14, secondary);
        result.setPadding(dp(16), dp(16), dp(16), dp(16));
        result.setBackground(rounded(surface, 18, outline));
        LinearLayout.LayoutParams resultLp = full();
        resultLp.topMargin = dp(16);
        root.addView(result, resultLp);

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
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) beginCamera();
            else showError("Camera permission is required to take a photo. You can still choose an existing image.");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) return;
        Uri uri = null;
        if (requestCode == NovaAndroidOperator.REQ_CAMERA) uri = pendingCameraUri;
        else if (requestCode == NovaAndroidOperator.REQ_IMAGE && data != null) uri = data.getData();
        if (uri != null) analyse(uri);
    }

    private void analyse(Uri uri) {
        NovaApiClient api = NovaSessionBridge.api();
        if (api == null || !api.isSignedIn()) {
            showError("Your active Nova session is not available in this screen. Return to Nova and open Vision again.");
            return;
        }
        camera.setEnabled(false);
        gallery.setEnabled(false);
        result.setTextColor(Color.rgb(169, 185, 211));
        result.setText("Analysing the photo with Nova Vision…");
        String hintText = hint.getText().toString().trim();
        worker.execute(() -> {
            try {
                String dataUrl = NovaAndroidOperator.imageDataUrl(this, uri);
                String formatted = NovaAndroidOperator.formatVision(api.vision(dataUrl, hintText));
                runOnUiThread(() -> {
                    result.setTextColor(Color.rgb(239, 244, 255));
                    result.setText(formatted);
                    camera.setEnabled(true);
                    gallery.setEnabled(true);
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

    private void showError(String message) {
        result.setTextColor(Color.rgb(255, 103, 122));
        result.setText(message == null || message.isBlank() ? "Nova Vision could not complete that photo." : message);
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

    private LinearLayout.LayoutParams full() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        worker.shutdownNow();
        super.onDestroy();
    }
}
