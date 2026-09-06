package com.buysloans.nova;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public final class MainActivity extends Activity implements UpdateManager.Listener {
    private TextView updateStatus;
    private Button updateButton;
    private UpdateManager updateManager;
    private UpdateManager.UpdateInfo pendingUpdate;

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }

    private TextView text(String value, float sizeSp, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sizeSp);
        view.setTextColor(color);
        return view;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final int background = Color.rgb(8, 12, 22);
        final int surface = Color.rgb(19, 27, 43);
        final int primary = Color.rgb(237, 241, 249);
        final int secondary = Color.rgb(166, 178, 199);
        final int accent = Color.rgb(124, 156, 255);
        final int success = Color.rgb(92, 214, 151);

        updateManager = new UpdateManager(this, this);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(background);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(this, 24), dp(this, 40), dp(this, 24), dp(this, 32));
        scroll.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));

        TextView brand = text("NOVA AI", 14, accent);
        brand.setLetterSpacing(0.18f);
        root.addView(brand);

        TextView title = text("Nova Control", 32, primary);
        title.setPadding(0, dp(this, 8), 0, dp(this, 4));
        root.addView(title);

        TextView subtitle = text("Standalone Morley intelligence companion", 16, secondary);
        subtitle.setPadding(0, 0, 0, dp(this, 28));
        root.addView(subtitle);

        LinearLayout statusCard = new LinearLayout(this);
        statusCard.setOrientation(LinearLayout.VERTICAL);
        statusCard.setPadding(dp(this, 20), dp(this, 20), dp(this, 20), dp(this, 20));
        statusCard.setBackgroundColor(surface);
        root.addView(statusCard, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView statusLabel = text("APP STATUS", 12, secondary);
        statusLabel.setLetterSpacing(0.12f);
        statusCard.addView(statusLabel);

        TextView status = text("●  Standalone shell ready", 20, success);
        status.setPadding(0, dp(this, 10), 0, dp(this, 8));
        statusCard.addView(status);

        TextView version = text("Nova Android " + BuildConfig.VERSION_NAME + "\nPackage: " + getPackageName(), 14, secondary);
        statusCard.addView(version);

        TextView updateTitle = text("OTA updates", 20, primary);
        updateTitle.setPadding(0, dp(this, 28), 0, dp(this, 8));
        root.addView(updateTitle);

        updateStatus = text("Secure update channel ready. APK downloads are SHA-256 verified before Android is asked to install them.", 14, secondary);
        updateStatus.setLineSpacing(0, 1.2f);
        root.addView(updateStatus);

        updateButton = new Button(this);
        updateButton.setText("Check for updates");
        updateButton.setAllCaps(false);
        updateButton.setOnClickListener(v -> {
            if (pendingUpdate == null) {
                updateManager.checkForUpdates();
            } else {
                updateManager.downloadAndInstall(pendingUpdate);
            }
        });
        LinearLayout.LayoutParams updateParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        updateParams.topMargin = dp(this, 14);
        root.addView(updateButton, updateParams);

        TextView boundaryTitle = text("Protected by design", 20, primary);
        boundaryTitle.setPadding(0, dp(this, 28), 0, dp(this, 8));
        root.addView(boundaryTitle);

        TextView boundary = text(
                "Nova runs as an isolated Android app. Production signing, release controls, Guardian boundaries and Morley Admin remain separate until explicitly connected through approved interfaces.",
                15,
                secondary);
        boundary.setLineSpacing(0, 1.25f);
        root.addView(boundary);

        TextView footer = text("Backed by Nova AI", 13, secondary);
        footer.setGravity(Gravity.CENTER_HORIZONTAL);
        footer.setPadding(0, dp(this, 40), 0, 0);
        root.addView(footer);

        View spacer = new View(this);
        root.addView(spacer, new LinearLayout.LayoutParams(1, dp(this, 8)));

        setContentView(scroll);
        updateManager.checkForUpdates();
    }

    @Override
    public void onStatus(String message) {
        updateStatus.setText(message);
        updateButton.setEnabled(true);
    }

    @Override
    public void onUpdateAvailable(UpdateManager.UpdateInfo info) {
        pendingUpdate = info;
        String mandatory = info.mandatory ? "\nThis update is marked mandatory." : "";
        updateStatus.setText("Nova " + info.versionName + " is available.\n" + info.notes + mandatory);
        updateButton.setText("Install update " + info.versionName);
        updateButton.setEnabled(true);
    }

    @Override
    public void onUpToDate() {
        pendingUpdate = null;
        updateStatus.setText("Nova is up to date.");
        updateButton.setText("Check again");
        updateButton.setEnabled(true);
    }

    @Override
    public void onError(String message) {
        updateStatus.setText(message);
        updateButton.setText("Retry update check");
        updateButton.setEnabled(true);
    }
}
