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

/**
 * Native Nova Control Center.
 *
 * The screen deliberately does not invent background-task progress or service-health state.
 * Live Morley facts are requested through NovaAssistantEngine, while local/session/policy facts
 * are rendered directly from the signed-in session and Android permission state.
 */
public final class NovaControlCenterActivity extends Activity {
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private TextView result;
    private NovaAssistantEngine assistant;

    private int primary;
    private int secondary;
    private int muted;
    private int accent;
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
        surfaceRaised = Color.rgb(20, 14, 34);
        outline = Color.rgb(52, 38, 78);
        success = Color.rgb(77, 224, 155);
        danger = Color.rgb(255, 103, 145);

        NovaApiClient api = NovaSessionBridge.api();
        if (api != null && api.isSignedIn()) assistant = new NovaAssistantEngine(api);
        buildUi();
    }

    @Override
    protected void onDestroy() {
        worker.shutdownNow();
        super.onDestroy();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(4, 2, 9));

        LinearLayout root = vertical();
        root.setPadding(dp(16), dp(20), dp(16), dp(36));
        scroll.addView(root);

        addBrand(root);
        addHero(root);
        addQuickActions(root);
        addKpis(root);
        addEvidenceChecks(root);
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

        boolean signedIn = assistant != null;
        row.addView(pill(signedIn ? "● AUTHORISED" : "● SESSION REQUIRED",
                signedIn ? success : danger,
                signedIn ? Color.rgb(9, 34, 28) : Color.rgb(42, 14, 27)));
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
        hero.addView(text("Faster  •  Smarter  •  Deeper  •  Evidence-backed", 13, secondary));

        result = text(
                assistant == null
                        ? "Return to the signed-in Nova screen and reopen Control Center."
                        : "I’m connected to your authorised Nova session. Choose a live evidence check or ask a Morley question.",
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
        addSectionTitle(root, "Quick actions", "Each action queries the authorised Nova engine; nothing below claims background work that has not been verified.");
        addActionRow(root,
                "Ask Nova", "What needs my attention?",
                "Find Devices", "Find missing catalogue devices and explain the current evidence");
        addActionRow(root,
                "Analyse Pricing", "pricing intelligence",
                "Check Images", "missing catalogue images");
    }

    private void addKpis(LinearLayout root) {
        addSectionTitle(root, "Authority & availability", "Local facts Nova can verify without pretending remote services are healthy.");
        boolean signedIn = assistant != null;
        boolean alerts = NovaAndroidOperator.notificationPermissionGranted(this);

        LinearLayout row1 = horizontal();
        row1.addView(metric("Session", signedIn ? "AUTHORISED" : "REQUIRED", signedIn ? success : danger), weightedCard());
        row1.addView(metric("Guardian", "HUMAN-GATED", accent), weightedCardEnd());
        root.addView(row1, full());

        LinearLayout row2 = horizontal();
        row2.setPadding(0, dp(8), 0, 0);
        row2.addView(metric("Alerts", alerts ? "ENABLED" : "PERMISSION", alerts ? success : secondary), weightedCard());
        row2.addView(metric("OTA", "SIGNED / GUARDED", accent), weightedCardEnd());
        root.addView(row2, full());
    }

    private void addEvidenceChecks(LinearLayout root) {
        addSectionTitle(root, "Live evidence checks", "Run a check when you need current evidence. Nova does not display fabricated completion percentages.");
        LinearLayout card = card();
        addEvidenceAction(card, "Catalogue coverage", "Check catalogue health and missing model data", "catalogue health");
        addEvidenceAction(card, "Pricing intelligence", "Read the latest available pricing evidence", "pricing intelligence");
        addEvidenceAction(card, "Support queue", "Check urgency, duplicates and SLA pressure", "support queue");
        addEvidenceAction(card, "Guardian state", "Read current protected Guardian evidence", "guardian status");
        addEvidenceAction(card, "Release readiness", "Read current release and parity evidence", "release state");
        addEvidenceAction(card, "Business performance", "Read current business intelligence", "business performance and profit");
        root.addView(card, full());
    }

    private void addInsights(LinearLayout root) {
        addSectionTitle(root, "Insights & Suggestions", "These are questions, not pre-claimed findings. Tap one to ask Nova for current evidence.");
        LinearLayout card = card();
        addInsight(card, "Missing devices", "Review catalogue evidence", "catalogue health");
        addInsight(card, "Pricing opportunities", "Review pricing intelligence", "pricing intelligence");
        addInsight(card, "Duplicate risk", "Ask for duplicate evidence", "catalogue duplicates");
        addInsight(card, "Image gaps", "Ask for image evidence", "missing catalogue images");
        root.addView(card, full());
    }

    private void addSystemStatus(LinearLayout root) {
        addSectionTitle(root, "System & protection state", "Only locally verifiable availability and fixed approval boundaries are shown as status.");
        LinearLayout card = card();
        boolean signedIn = assistant != null;
        boolean alerts = NovaAndroidOperator.notificationPermissionGranted(this);
        addStatus(card, "Nova session", signedIn ? "Authorised" : "Unavailable", signedIn ? success : danger);
        addStatus(card, "Guardian approvals", "Human-gated", accent);
        addStatus(card, "Protected writes", "Approval required", accent);
        addStatus(card, "Device Vision", "Available", success);
        addStatus(card, "Proactive alerts", alerts ? "Permission granted" : "Permission required", alerts ? success : secondary);
        addStatus(card, "Signed OTA", "Guarded release channel", accent);
        root.addView(card, full());

        Button vision = button("Open device Vision", true);
        vision.setOnClickListener(v -> startActivity(new Intent(this, NovaVisionActivity.class)));
        LinearLayout.LayoutParams visionLp = full();
        visionLp.topMargin = dp(10);
        root.addView(vision, visionLp);

        Button alertsButton = button(alerts ? "Proactive alerts enabled" : "Enable proactive alerts", false);
        alertsButton.setOnClickListener(v -> {
            NovaAndroidOperator.scheduleBackgroundAlerts(this);
            NovaAndroidOperator.requestNotificationPermission(this);
            alertsButton.setText(
                    NovaAndroidOperator.notificationPermissionGranted(this)
                            ? "Proactive alerts enabled"
                            : "Allow notifications to enable alerts"
            );
        });
        LinearLayout.LayoutParams alertsLp = full();
        alertsLp.topMargin = dp(8);
        root.addView(alertsButton, alertsLp);
    }

    private void addCommandBar(LinearLayout root) {
        addSectionTitle(root, "Ask Nova anything", "Live Morley questions stay inside the authorised Nova session.");
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

    private void addEvidenceAction(LinearLayout card, String label, String helper, String query) {
        Button b = button(label + "\n" + helper, false);
        b.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        b.setPadding(dp(14), dp(10), dp(14), dp(10));
        b.setOnClickListener(v -> run(query, b, label + "\n" + helper));
        LinearLayout.LayoutParams lp = full();
        lp.bottomMargin = dp(8);
        card.addView(b, lp);
    }

    private void addInsight(LinearLayout card, String label, String helper, String query) {
        addEvidenceAction(card, label, helper, query);
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
        result.setText("Reading authorised Morley evidence…");
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
        b.setTextAllCaps(false);
        b.setTextSize(12);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setTextColor(primary);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(10), dp(10), dp(10), dp(10));
        b.setMinHeight(dp(48));
        b.setBackground(rounded(
                important ? Color.rgb(88, 43, 166) : Color.rgb(27, 18, 46),
                14,
                important ? accent : outline
        ));
        return b;
    }

    private GradientDrawable rounded(int fill, int radiusDp, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(radiusDp));
        if (stroke != Color.TRANSPARENT) d.setStroke(dp(1), stroke);
        return d;
    }

    private GradientDrawable gradientCard() {
        GradientDrawable d = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(35, 21, 59), Color.rgb(12, 8, 22)}
        );
        d.setCornerRadius(dp(22));
        d.setStroke(dp(1), Color.rgb(80, 45, 120));
        return d;
    }

    private LinearLayout.LayoutParams full() {
        return new LinearLayout.LayoutParams(match(), wrap());
    }

    private LinearLayout.LayoutParams weightedCard() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, wrap(), 1f);
        lp.rightMargin = dp(4);
        return lp;
    }

    private LinearLayout.LayoutParams weightedCardEnd() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, wrap(), 1f);
        lp.leftMargin = dp(4);
        return lp;
    }

    private LinearLayout.LayoutParams weightedButton() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(58), 1f);
        lp.rightMargin = dp(4);
        lp.bottomMargin = dp(8);
        return lp;
    }

    private LinearLayout.LayoutParams weightedButtonEnd() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(58), 1f);
        lp.leftMargin = dp(4);
        lp.bottomMargin = dp(8);
        return lp;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private int match() {
        return LinearLayout.LayoutParams.MATCH_PARENT;
    }

    private int wrap() {
        return LinearLayout.LayoutParams.WRAP_CONTENT;
    }
}
