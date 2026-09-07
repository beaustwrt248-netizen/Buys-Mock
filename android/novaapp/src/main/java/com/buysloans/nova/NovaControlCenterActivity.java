package com.buysloans.nova;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class NovaControlCenterActivity extends Activity {
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private TextView result;
    private NovaAssistantEngine assistant;
    private int primary, secondary, accent, surface, outline, danger;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        primary = Color.rgb(239, 244, 255);
        secondary = Color.rgb(169, 185, 211);
        accent = Color.rgb(82, 139, 255);
        surface = Color.rgb(7, 23, 42);
        outline = Color.rgb(38, 66, 101);
        danger = Color.rgb(255, 103, 122);
        NovaApiClient api = NovaSessionBridge.api();
        if (api != null && api.isSignedIn()) assistant = new NovaAssistantEngine(api);
        buildUi();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(2, 10, 22));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(24), dp(18), dp(32));
        scroll.addView(root);

        TextView brand = text("NOVA AI  •  CONTROL CENTRE", 14, accent);
        brand.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        brand.setLetterSpacing(.16f);
        root.addView(brand);
        TextView title = text("Morley operator view", 24, primary);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setPadding(0, dp(12), 0, dp(4));
        root.addView(title);
        root.addView(text("One live Android surface for Guardian, support, catalogue, inventory, business, releases, learning and image intelligence. Protected approvals and production changes remain human-gated.", 13, secondary));

        result = text(assistant == null ? "Return to the signed-in Nova screen and reopen the control centre." : "Choose a live view below.", 14, secondary);
        result.setPadding(dp(15), dp(15), dp(15), dp(15));
        result.setBackground(rounded(surface, 18, outline));
        LinearLayout.LayoutParams resultLp = full();
        resultLp.topMargin = dp(16);
        root.addView(result, resultLp);

        addAction(root, "What needs attention?", "What needs my attention?");
        addRow(root, "Guardian", "guardian status", "Support", "support queue");
        addRow(root, "Catalogue", "catalogue health", "Inventory", "inventory health");
        addRow(root, "Business", "business performance and profit", "Releases", "release state");
        addRow(root, "Learning", "nova learning accuracy", "Daily brief", "Give me the daily brief");

        Button vision = button("Open device Vision", true);
        vision.setOnClickListener(v -> startActivity(new Intent(this, NovaVisionActivity.class)));
        LinearLayout.LayoutParams visionLp = full();
        visionLp.topMargin = dp(10);
        root.addView(vision, visionLp);

        Button back = button("Back to Nova", false);
        back.setOnClickListener(v -> finish());
        LinearLayout.LayoutParams backLp = full();
        backLp.topMargin = dp(10);
        root.addView(back, backLp);
        setContentView(scroll);
    }

    private void addAction(LinearLayout root, String label, String query) {
        Button b = button(label, true);
        b.setOnClickListener(v -> run(query, b, label));
        LinearLayout.LayoutParams lp = full();
        lp.topMargin = dp(12);
        root.addView(b, lp);
    }

    private void addRow(LinearLayout root, String leftLabel, String leftQuery, String rightLabel, String rightQuery) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        Button left = button(leftLabel, false);
        Button right = button(rightLabel, false);
        left.setOnClickListener(v -> run(leftQuery, left, leftLabel));
        right.setOnClickListener(v -> run(rightQuery, right, rightLabel));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(50), 1f);
        lp.setMarginEnd(dp(4));
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(0, dp(50), 1f);
        rp.setMarginStart(dp(4));
        row.addView(left, lp);
        row.addView(right, rp);
        LinearLayout.LayoutParams rowLp = full();
        rowLp.topMargin = dp(8);
        root.addView(row, rowLp);
    }

    private void run(String query, Button trigger, String label) {
        if (assistant == null) {
            result.setTextColor(danger);
            result.setText("The active Nova session is unavailable. Return to Nova and reopen this screen.");
            return;
        }
        trigger.setEnabled(false);
        result.setTextColor(secondary);
        result.setText("Reading live Morley data…");
        worker.execute(() -> {
            try {
                String value = assistant.answer(query);
                runOnUiThread(() -> {
                    result.setTextColor(primary);
                    result.setText(value);
                    trigger.setEnabled(true);
                    trigger.setText(label);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    result.setTextColor(danger);
                    result.setText("That authorised live source is unavailable right now.");
                    trigger.setEnabled(true);
                    trigger.setText(label);
                });
            }
        });
    }

    private TextView text(String value, float size, int color) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(size);
        v.setTextColor(color);
        v.setLineSpacing(0, 1.08f);
        return v;
    }

    private Button button(String label, boolean important) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextColor(primary);
        b.setTextSize(13);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setGravity(Gravity.CENTER);
        b.setBackground(important ? rounded(Color.rgb(61, 100, 224), 16, accent) : rounded(surface, 16, outline));
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

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    @Override
    protected void onDestroy() {
        worker.shutdownNow();
        super.onDestroy();
    }
}
