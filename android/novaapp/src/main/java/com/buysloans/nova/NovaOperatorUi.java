package com.buysloans.nova;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.lang.reflect.Field;

final class NovaOperatorUi {
    private static final int CONTROL_ID = 0x4e4f5641;

    private NovaOperatorUi() {}

    static void install(Activity activity) {
        if (!(activity instanceof MainActivity)) return;
        attachSession(activity);
        View decor = activity.getWindow().getDecorView();
        if (!(decor instanceof ViewGroup)) return;
        boolean intelligence = hasPlainText((ViewGroup) decor, "Intelligence");
        View existing = decor.findViewById(CONTROL_ID);
        if (!intelligence) {
            if (existing != null && existing.getParent() instanceof ViewGroup) ((ViewGroup) existing.getParent()).removeView(existing);
            return;
        }
        if (existing != null) return;

        FrameLayout content = activity.findViewById(android.R.id.content);
        if (content == null) return;
        Button button = new Button(activity);
        button.setId(CONTROL_ID);
        button.setText("◈");
        button.setContentDescription("Open Nova control centre");
        button.setAllCaps(false);
        button.setTextSize(18);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(Color.rgb(239, 244, 255));
        button.setMinWidth(0);
        button.setMinHeight(0);
        button.setPadding(0, 0, 0, 0);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(Color.rgb(61, 100, 224));
        bg.setStroke(dp(activity, 1), Color.rgb(143, 91, 255));
        button.setBackground(bg);
        button.setOnClickListener(v -> activity.startActivity(new Intent(activity, NovaControlCenterActivity.class)));

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(dp(activity, 52), dp(activity, 52));
        lp.gravity = Gravity.END | Gravity.BOTTOM;
        lp.setMarginEnd(dp(activity, 18));
        lp.bottomMargin = dp(activity, 88);
        content.addView(button, lp);
    }

    static void clearSession() { NovaSessionBridge.clear(); }

    private static void attachSession(Activity activity) {
        try {
            Field field = MainActivity.class.getDeclaredField("api");
            field.setAccessible(true);
            Object value = field.get(activity);
            if (value instanceof NovaApiClient) NovaSessionBridge.attach((NovaApiClient) value);
        } catch (Exception ignored) {}
    }

    private static boolean hasPlainText(ViewGroup root, String exact) {
        for (int i = 0; i < root.getChildCount(); i++) {
            View child = root.getChildAt(i);
            if (child instanceof TextView && !(child instanceof Button)) {
                CharSequence text = ((TextView) child).getText();
                if (text != null && exact.contentEquals(text)) return true;
            }
            if (child instanceof ViewGroup && hasPlainText((ViewGroup) child, exact)) return true;
        }
        return false;
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
