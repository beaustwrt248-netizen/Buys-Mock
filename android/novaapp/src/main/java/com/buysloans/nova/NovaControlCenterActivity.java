package com.buysloans.nova;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class NovaControlCenterActivity extends Activity {
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private TextView result;
    private NovaAssistantEngine assistant;

    private int primary;
    private int secondary;
    private int muted;
    private int accent;
    private int accentSoft;
    private int surface;
    private int surfaceRaised;
    private int outline;
    private int success;
    private int danger;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        primary = Color.rgb(247, 245, 255);
        secondary = Color.rgb(190, 184, 214);
        muted = Color.rgb(139, 132, 165);
        accent = Color.rgb(151, 78, 255);
        accentSoft = Color.rgb(77, 43, 123);
        surface = Color.rgb(12, 8, 22);
        surfaceRaised = Color.rgb(20, 14, 34);
        outline = Color.rgb(52, 38, 78);
        success = Color.rgb(77, 224, 155);
        danger = Color.rgb(255, 103, 145);

        NovaApiClient api = NovaSessionBridge.api();
        if (api != null && api.isSignedIn()) assistant = new NovaAssistantEngine(api);
        buildUi();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(4, 2, 9));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(20), dp(16), dp(36));
        scroll.addView(root);

        addBrand(root);
        addHero(root);
        addQuickActions(root);
        addKpis(root);
        addLiveTasks(root);
        addInsights(root);
        addSystemStatus(root);
        addCommandBar(root);
        addFooter(root);

        setContentView(scroll);
    }

    private void addBrand(LinearLayout root) {
        LinearLayout row = horizontal();
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView brand = text("NOVA AI", 18, primary);
        brand.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        brand.setLetterSpacing(.10f);
        row.addView(brand, new LinearLayout.LayoutParams(0, wrap(), 1f));

        TextView status = pill("● ONLINE", success, Color.rgb(9, 34, 28));
        row.addView(status);
        root.addView(row);

        TextView sub = text("CONTROL CENTER", 11, accent);
        sub.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        sub.setLetterSpacing(.20f);
        sub.setPadding(0, dp(2), 0, dp(14));
        root.addView(sub);
    }

    private void addHero(LinearLayout root) {
        LinearLayout hero = vertical();
        hero.setPadding(dp(18), dp(18), dp(18), dp(18));
        hero.setBackground(gradientCard());

        TextView eyebrow = text("MORLEY INTELLIGENCE", 11, accent);
        eyebrow.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        eyebrow.setLetterSpacing(.13f);
        hero.addView(eyebrow);

        TextView title = text("Hello Beau, I’m Nova!", 26, primary);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setPadding(0, dp(8), 0, dp(6));
        hero.addView(title);

        hero.addView(text("Faster  •  Smarter  •  Deeper  •  Always On", 13, secondary));

        result = text(
                assistant == null
                        ? "Return to the signed-in Nova screen and reopen Control Center."
                        : "I’m connected to live Morley data. Choose an action or ask me anything.",
                13,
                assistant == null ? danger : secondary
        );
        result.setPadding(dp(14), dp(14), dp(14), dp(14));
        result.setBackground(rounded(Color.rgb(10, 7, 18), 16, outline));
        LinearLayout.LayoutParams resultLp = full();
        resultLp.topMargin = dp(14);
        hero.addView(result, resultLp);

        root.addView(hero, full());
    }

    private void addQuickActions(LinearLayout root) {
        addSectionTitle(root, "Quick actions", "Move straight into Nova’s most-used tools.");
        addActionRow(root,
                "Ask Nova", "What needs my attention?",
                "Find Devices", "Find missing catalogue devices");
        addActionRow(root,
                "Analyse Pricing", "pricing intelligence",
                "Check Images", "image intelligence");
    }

    private void addKpis(LinearLayout root) {
        addSectionTitle(root, "Live intelligence", "Key signals from the Morley operating layer.");
        LinearLayout row1 = horizontal();
        row1.addView(metric("Catalogue", "LIVE", success), weightedCard());
        row1.addView(metric("Guardian", "PROTECTED", accent), weightedCardEnd());
        root.addView(row1, full());

        LinearLayout row2 = horizontal();
        row2.setPadding(0, dp(8), 0, 0);
        row2.addView(metric("Support", "MONITORING", success), weightedCard());
        row2.addView(metric("Release", "GUARDED", accent), weightedCardEnd());
        root.addView(row2, full());
    }

    private void addLiveTasks(LinearLayout root) {
        addSectionTitle(root, "Live Tasks", "Nova keeps each task visible instead of hiding background work.");
        LinearLayout card = card();
        addTask(card, "Retailer scanning", 78, "AU manufacturer + retailer sources");
        addTask(card, "Missing images", 62, "Catalogue image verification");
        addTask(card, "Pricing analysis", 54, "Marketplace + direct seller comparison");
        addTask(card, "Support review", 86, "Urgency, duplicates and SLA risk");
        addTask(card, "Device learning", 69, "Model, storage and release-year validation");
        addTask(card, "Web / app monitoring", 91, "Release, parity and runtime checks");
        root.addView(card, full());
    }

    private void addInsights(LinearLayout root) {
        addSectionTitle(root, "Insights & Suggestions", "Tap a signal to ask Nova for the evidence behind it.");
        LinearLayout card = card();
        addInsight(card, "Missing devices", "Review catalogue health", "catalogue health");
        addInsight(card, "Underpriced stock", "Review pricing intelligence", "pricing intelligence");
        addInsight(card, "Duplicates", "Explain duplicate risk", "catalogue duplicates");
        addInsight(card, "Missing images", "Find image gaps", "missing catalogue images");
        root.addView(card, full());
    }

    private void addSystemStatus(LinearLayout root) {
        addSectionTitle(root, "System Status", "Protected services and approval boundaries stay visible.");
        LinearLayout card = card();
        addStatus(card, "Web Monitoring", "Connected", success);
        addStatus(card, "Data Research", "Ready", success);
        addStatus(card, "Catalogue Sync", "Protected", accent);
        addStatus(card, "AI Model", "Online", success);
        addStatus(card, "API Connections", "Authorised", success);
        addStatus(card, "Scheduled Tasks", "Active", success);
        addStatus(card, "Guardian approvals", "Human-gated", accent);
        root.addView(card, full());

        Button vision = button("Open device Vision", true);
        vision.setOnClickListener(v -> startActivity(new Intent(this, NovaVisionActivity.class)));
        LinearLayout.LayoutParams visionLp = full();
        visionLp.topMargin = dp(10);
        root.addView(vision, visionLp);

        Button alerts = button(
                NovaAndroidOperator.notificationPermissionGranted(this)
                        ? "Proactive alerts enabled"
                        : "Enable proactive alerts",
                false
        );
        alerts.setOnClickListener(v -> {
            NovaAndroidOperator.scheduleBackgroundAlerts(this);
            NovaAndroidOperator.requestNotificationPermission(this);
            alerts.setText(
                    NovaAndroidOperator.notificationPermissionGranted(this)
                            ? "Proactive alerts enabled"
                            : "Allow notifications to enable alerts"
            );
        });
        LinearLayout.LayoutParams alertsLp = full();
        alertsLp.topMargin = dp(8);
        root.addView(alerts, alertsLp);
    }

    private void addCommandBar(LinearLayout root) {
        addSectionTitle(root, "Ask Nova anything", "Live Morley questions stay inside the signed-in Nova session.");
        LinearLayout card = card();
        addCommandChip(card, "What needs attention?", "What needs my attention?");
        addCommandChip(card, "Guardian status", "guardian status");
        addCommandChip(card, "Support queue", "support queue");
        addCommandChip(card, "Business performance", "business performance and profit");
        addCommandChip(card, "Release state", "release state");
        addCommandChip(card, "Daily brief", "Give me the daily brief");
        root.addView(card, full());
    }

    private void addFooter(LinearLayout root) {
        TextView footer = text("NOVA AI  |  Backed by Morley Buys\nSmarter Data. Stronger Decisions.\nGuardian protected · risky changes require human approval", 11, muted);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, dp(22), 0, dp(8));
        root.addView(footer, full());

        Button back = button("Back to Nova", false);
        back.setOnClickListener(v -> finish());
        root.addView(back, full());
    }

    private void addSectionTitle(LinearLayout root, String title, String subtitle) {
        TextView heading = text(title, 18, primary);
        heading.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        heading.setPadding(0, dp(20), 0, dp(3));
        root.addView(heading);
        TextView helper = text(subtitle, 12, muted);
        helper.setPadding(0, 0, 0, dp(9));
        root.addView(helper);
    }

    private void addActionRow(LinearLayout root, String leftLabel, String leftQuery, String rightLabel, String rightQuery) {
        LinearLayout row = horizontal();
        Button left = button(leftLabel, true);
        Button right = button(rightLabel, false);
        left.setOnClickListener(v -> run(leftQuery, left, leftLabel));
        right.setOnClickListener(v -> run(rightQuery, right, rightLabel));
        row.addView(left, weightedButton());
        row.addView(right, weightedButtonEnd());
        root.addView(row, full());
    }

    private LinearLayout metric(String label, String value, int valueColor) {
        LinearLayout box = card();
        box.setPadding(dp(14), dp(14), dp(14), dp(14));
        TextView labelView = text(label.toUpperCase(), 10, muted);
        labelView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        labelView.setLetterSpacing(.10f);
        box.addView(labelView);
        TextView valueView = text(value, 14, valueColor);
        valueView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        valueView.setPadding(0, dp(5), 0, 0);
        box.addView(valueView);
        return box;
    }

    private void addTask(LinearLayout card, String name, int progress, String detail) {
        LinearLayout header = horizontal();
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text(name, 13, primary);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0, wrap(), 1f));
        header.addView(text(progress + "%", 12, accent));
        card.addView(header, full());

        ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        bar.setProgress(progress);
        bar.getProgressDrawable().setTint(accent);
        bar.getProgressDrawable().setTintBlendMode(android.graphics.BlendMode.SRC_IN);
        LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(match(), dp(7));
        barLp.topMargin = dp(5);
        card.addView(bar, barLp);

        TextView detailView = text(detail, 11, muted);
        detailView.setPadding(0, dp(4), 0, dp(12));
        card.addView(detailView);
    }

    private void addInsight(LinearLayout card, String label, String helper, String query) {
        Button b = button(label + "\n" + helper, false);
        b.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        b.setPadding(dp(14), dp(10), dp(14), dp(10));
        b.setOnClickListener(v -> run(query, b, label + "\n" + helper));
        LinearLayout.LayoutParams lp = full();
        lp.bottomMargin = dp(8);
        card.addView(b, lp);
    }

    private void addStatus(LinearLayout card, String label, String value, int dotColor) {
        LinearLayout row = horizontal();
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView name = text(label, 13, primary);
        row.addView(name, new LinearLayout.LayoutParams(0, wrap(), 1f));
        TextView valueView = text("●  " + value, 12, dotColor);
        valueView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        row.addView(valueView);
        LinearLayout.LayoutParams lp = full();
        lp.bottomMargin = dp(10);
        card.addView(row, lp);
    }

    private void addCommandChip(LinearLayout card, String label, String query) {
        Button chip = button(label, false);
        chip.setOnClickListener(v -> run(query, chip, label));
        LinearLayout.LayoutParams lp = full();
        lp.bottomMargin = dp(7);
        card.addView(chip, lp);
    }

    private void run(String query, Button trigger, String label) {
        if (assistant == null) {
            result.setTextColor(danger);
            result.setText("The active Nova session is unavailable. Return to Nova and reopen this screen.");
            return;
        }
        trigger.setEnabled(false);
        trigger.setAlpha(.72f);
        result.setTextColor(secondary);
        result.setText("Reading live Morley data…");
        worker.execute(() -> {
            try {
                String value = assistant.answer(query);
                runOnUiThread(() -> {
                    result.setTextColor(primary);
                    result.setText(value);
                    trigger.setEnabled(true);
                    trigger.setAlpha(1f);
                    trigger.setText(label);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    result.setTextColor(danger);
                    result.setText("That authorised live source is unavailable right now.");
                    trigger.setEnabled(true);
                    trigger.setAlpha(1f);
                    trigger.setText(label);
                });
            }
        });
    }

    private LinearLayout card() {
        LinearLayout card = vertical();
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        card.setBackground(rounded(surfaceRaised, 18, outline));
        return card;
    }

    private LinearLayout horizontal() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        return row;
    }

    private LinearLayout vertical() {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        return col;
    }

    private TextView text(String value, float size, int color) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(size);
        v.setTextColor(color);
        v.setLineSpacing(0, 1.08f);
        return v;
    }

    private TextView pill(String value, int textColor, int background) {
        TextView v = text(value, 10, textColor);
        v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        v.setGravity(Gravity.CENTER);
        v.setPadding(dp(10), dp(6), dp(10), dp(6));
        v.setBackground(rounded(background, 999, Color.TRANSPARENT));
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
        b.setMinHeight(dp(48));
        b.setBackground(important ? rounded(accentSoft, 16, accent) : rounded(surface, 16, outline));
        return b;
    }

    private GradientDrawable gradientCard() {
        GradientDrawable d = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(41, 19, 68), Color.rgb(20, 10, 38), Color.rgb(12, 7, 24)}
        );
        d.setCornerRadius(dp(22));
        d.setStroke(dp(1), Color.rgb(87, 48, 130));
        return d;
    }

    private GradientDrawable rounded(int fill, int radius, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(radius));
        if (stroke != Color.TRANSPARENT) d.setStroke(dp(1), stroke);
        return d;
    }

    private LinearLayout.LayoutParams full() {
        return new LinearLayout.LayoutParams(match(), wrap());
    }

    private LinearLayout.LayoutParams weightedButton() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(52), 1f);
        lp.setMarginEnd(dp(4));
        lp.bottomMargin = dp(8);
        return lp;
    }

    private LinearLayout.LayoutParams weightedButtonEnd() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(52), 1f);
        lp.setMarginStart(dp(4));
        lp.bottomMargin = dp(8);
        return lp;
    }

    private LinearLayout.LayoutParams weightedCard() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, wrap(), 1f);
        lp.setMarginEnd(dp(4));
        return lp;
    }

    private LinearLayout.LayoutParams weightedCardEnd() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, wrap(), 1f);
        lp.setMarginStart(dp(4));
        return lp;
    }

    private int match() {
        return LinearLayout.LayoutParams.MATCH_PARENT;
    }

    private int wrap() {
        return LinearLayout.LayoutParams.WRAP_CONTENT;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        worker.shutdownNow();
        super.onDestroy();
    }
}
