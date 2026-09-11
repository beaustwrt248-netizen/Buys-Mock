package com.buysloans.nova;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Locale;
import java.util.WeakHashMap;

final class NovaOperatorUi {
    private static final int CONTROL_ID = 0x4e4f5641;
    private static final int MODEL_BAR_ID = 0x4e4f5642;
    private static final int MODEL_STATUS_ID = 0x4e4f5643;
    private static final int REQ_RECORD_AUDIO = 9190;
    private static final WeakHashMap<Activity, SpeechRecognizer> SPEECH = new WeakHashMap<>();
    private static final WeakHashMap<Activity, Boolean> LISTENING = new WeakHashMap<>();

    private NovaOperatorUi() {}

    static void install(Activity activity) {
        if (!(activity instanceof MainActivity)) return;
        NovaApiClient api = attachSession(activity);
        View decor = activity.getWindow().getDecorView();
        if (!(decor instanceof ViewGroup)) return;

        enhanceChat(activity, (ViewGroup) decor, api);

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

    static void clearSession() {
        NovaSessionBridge.clear();
        for (SpeechRecognizer recognizer : new ArrayList<>(SPEECH.values())) {
            try { recognizer.destroy(); } catch (Throwable ignored) {}
        }
        SPEECH.clear();
        LISTENING.clear();
    }

    private static NovaApiClient attachSession(Activity activity) {
        try {
            Field field = MainActivity.class.getDeclaredField("api");
            field.setAccessible(true);
            Object value = field.get(activity);
            if (value instanceof NovaApiClient) {
                NovaApiClient api = (NovaApiClient) value;
                NovaSessionBridge.attach(api);
                String saved = activity.getSharedPreferences("nova_ai", Activity.MODE_PRIVATE).getString("provider", "auto");
                api.setPreferredProvider(saved);
                return api;
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static void enhanceChat(Activity activity, ViewGroup decor, NovaApiClient api) {
        EditText input = findMessageInput(decor);
        if (input == null || api == null || !api.isSignedIn()) return;
        View parent = (View) input.getParent();
        if (!(parent instanceof LinearLayout)) return;
        LinearLayout composer = (LinearLayout) parent;
        if (!(composer.getParent() instanceof LinearLayout)) return;
        LinearLayout chatCard = (LinearLayout) composer.getParent();

        if (chatCard.findViewById(MODEL_BAR_ID) == null) {
            LinearLayout modelWrap = new LinearLayout(activity);
            modelWrap.setId(MODEL_BAR_ID);
            modelWrap.setOrientation(LinearLayout.VERTICAL);
            modelWrap.setPadding(0, dp(activity, 10), 0, dp(activity, 2));

            TextView label = new TextView(activity);
            label.setText("AI mode");
            label.setTextSize(11);
            label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            label.setTextColor(Color.rgb(169, 185, 211));
            modelWrap.addView(label);

            HorizontalScrollView scroll = new HorizontalScrollView(activity);
            scroll.setHorizontalScrollBarEnabled(false);
            LinearLayout row = new LinearLayout(activity);
            row.setOrientation(LinearLayout.HORIZONTAL);
            String current = api.preferredProvider();
            addModeButton(activity, row, api, "Auto", "auto", current);
            addModeButton(activity, row, api, "GPT", "gpt", current);
            addModeButton(activity, row, api, "Gemini", "gemini", current);
            addModeButton(activity, row, api, "Claude", "claude", current);
            addModeButton(activity, row, api, "Consensus", "consensus", current);
            scroll.addView(row);
            modelWrap.addView(scroll, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(activity, 46)));

            TextView status = new TextView(activity);
            status.setId(MODEL_STATUS_ID);
            status.setTextSize(10);
            status.setTextColor(Color.rgb(120, 147, 190));
            status.setPadding(dp(activity, 2), 0, dp(activity, 2), dp(activity, 3));
            status.setText("Auto chooses the lightest model path that fits the task. Consensus asks multiple models when requested.");
            modelWrap.addView(status);

            int composerIndex = chatCard.indexOfChild(composer);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            chatCard.addView(modelWrap, Math.max(0, composerIndex), lp);
        }

        Button mic = findButtonByContentDescription(decor, "Voice");
        if (mic != null && !Boolean.TRUE.equals(mic.getTag())) {
            mic.setTag(Boolean.TRUE);
            mic.setText("🎙");
            mic.setContentDescription("Voice input. Tap to speak; tap again to cancel.");
            mic.setOnClickListener(v -> toggleVoice(activity, mic, input));
        }

        updateRunStatus(chatCard, api);
    }

    private static void addModeButton(Activity activity, LinearLayout row, NovaApiClient api, String label, String value, String current) {
        Button button = new Button(activity);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(11);
        button.setMinWidth(0);
        button.setMinHeight(0);
        button.setPadding(dp(activity, 13), 0, dp(activity, 13), 0);
        boolean selected = value.equals(current);
        button.setTextColor(selected ? Color.WHITE : Color.rgb(190, 205, 230));
        button.setBackground(pill(activity, selected));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(activity, 38));
        lp.setMarginEnd(dp(activity, 6));
        row.addView(button, lp);
        button.setOnClickListener(v -> {
            api.setPreferredProvider(value);
            activity.getSharedPreferences("nova_ai", Activity.MODE_PRIVATE).edit().putString("provider", value).apply();
            View modelBar = activity.getWindow().getDecorView().findViewById(MODEL_BAR_ID);
            if (modelBar != null && modelBar.getParent() instanceof ViewGroup) {
                ((ViewGroup) modelBar.getParent()).removeView(modelBar);
            }
            View decor = activity.getWindow().getDecorView();
            decor.post(() -> install(activity));
        });
    }

    private static GradientDrawable pill(Activity activity, boolean selected) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(activity, 20));
        bg.setColor(selected ? Color.rgb(61, 100, 224) : Color.rgb(9, 31, 54));
        bg.setStroke(dp(activity, 1), selected ? Color.rgb(143, 91, 255) : Color.rgb(38, 66, 101));
        return bg;
    }

    private static void updateRunStatus(LinearLayout chatCard, NovaApiClient api) {
        TextView status = chatCard.findViewById(MODEL_STATUS_ID);
        if (status == null) return;
        JSONObject result = api.lastOrchestratorResult();
        if (result == null) return;
        JSONObject telemetry = result.optJSONObject("telemetry");
        JSONArray models = result.optJSONArray("models_used");
        String mode = result.optString("mode", "");
        String provider = result.optString("provider", api.preferredProvider());
        long latency = telemetry == null ? 0 : telemetry.optLong("latency_ms", 0);
        double cost = telemetry == null ? 0 : telemetry.optDouble("cost_usd", 0);
        StringBuilder summary = new StringBuilder();
        summary.append("Last AI run: ").append(provider.isBlank() ? "auto" : provider);
        if (!mode.isBlank()) summary.append(" • ").append(mode);
        if (models != null && models.length() > 0) {
            summary.append(" • ");
            for (int i = 0; i < models.length(); i++) {
                if (i > 0) summary.append(", ");
                summary.append(shortModel(models.optString(i)));
            }
        }
        if (latency > 0) summary.append(" • ").append(String.format(Locale.US, "%.1fs", latency / 1000d));
        if (cost > 0) summary.append(" • $").append(String.format(Locale.US, "%.4f", cost));
        if (mode.contains("degraded") || mode.contains("unfused")) summary.append(" • fallback");
        status.setText(summary.toString());
    }

    private static String shortModel(String model) {
        if (model == null) return "";
        int slash = model.indexOf('/');
        return slash >= 0 && slash + 1 < model.length() ? model.substring(slash + 1) : model;
    }

    private static void toggleVoice(Activity activity, Button mic, EditText input) {
        if (Boolean.TRUE.equals(LISTENING.get(activity))) {
            SpeechRecognizer recognizer = SPEECH.get(activity);
            if (recognizer != null) {
                try { recognizer.cancel(); } catch (Throwable ignored) {}
            }
            LISTENING.put(activity, false);
            mic.setText("🎙");
            return;
        }
        if (activity.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            activity.requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_RECORD_AUDIO);
            Toast.makeText(activity, "Allow microphone access, then tap the mic again.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!SpeechRecognizer.isRecognitionAvailable(activity)) {
            Toast.makeText(activity, "Speech recognition is unavailable on this device.", Toast.LENGTH_SHORT).show();
            return;
        }

        SpeechRecognizer recognizer = SPEECH.get(activity);
        if (recognizer == null) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(activity);
            SPEECH.put(activity, recognizer);
        }
        SpeechRecognizer finalRecognizer = recognizer;
        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) { LISTENING.put(activity, true); mic.setText("■"); }
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() { mic.setText("…"); }
            @Override public void onError(int error) {
                LISTENING.put(activity, false);
                mic.setText("🎙");
                if (error != SpeechRecognizer.ERROR_CLIENT && error != SpeechRecognizer.ERROR_NO_MATCH && error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                    Toast.makeText(activity, "Voice input could not complete. Try again.", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onResults(Bundle results) {
                LISTENING.put(activity, false);
                mic.setText("🎙");
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches == null || matches.isEmpty()) return;
                String spoken = matches.get(0).trim();
                if (spoken.isEmpty()) return;
                input.setText(spoken);
                input.setSelection(input.length());
                sendVoiceResult(activity, input);
            }
            @Override public void onPartialResults(Bundle partialResults) {
                ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String partial = matches.get(0).trim();
                    if (!partial.isEmpty()) {
                        input.setText(partial);
                        input.setSelection(input.length());
                    }
                }
            }
            @Override public void onEvent(int eventType, Bundle params) {}
        });

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag());
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to Nova");
        try {
            finalRecognizer.startListening(intent);
            LISTENING.put(activity, true);
            mic.setText("■");
        } catch (Throwable error) {
            LISTENING.put(activity, false);
            mic.setText("🎙");
            Toast.makeText(activity, "Voice input could not start.", Toast.LENGTH_SHORT).show();
        }
    }

    private static void sendVoiceResult(Activity activity, EditText input) {
        try {
            Method method = MainActivity.class.getDeclaredMethod("sendQuestion", EditText.class, Button.class);
            method.setAccessible(true);
            Button send = findButtonByText((ViewGroup) activity.getWindow().getDecorView(), "➤");
            method.invoke(activity, input, send);
        } catch (Throwable ignored) {
            Toast.makeText(activity, "Voice captured. Tap send when ready.", Toast.LENGTH_SHORT).show();
        }
    }

    private static EditText findMessageInput(ViewGroup root) {
        for (int i = 0; i < root.getChildCount(); i++) {
            View child = root.getChildAt(i);
            if (child instanceof EditText) {
                CharSequence hint = ((EditText) child).getHint();
                if (hint != null && hint.toString().contains("Message Nova")) return (EditText) child;
            }
            if (child instanceof ViewGroup) {
                EditText found = findMessageInput((ViewGroup) child);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static Button findButtonByContentDescription(ViewGroup root, String contains) {
        for (int i = 0; i < root.getChildCount(); i++) {
            View child = root.getChildAt(i);
            if (child instanceof Button) {
                CharSequence description = child.getContentDescription();
                if (description != null && description.toString().contains(contains)) return (Button) child;
            }
            if (child instanceof ViewGroup) {
                Button found = findButtonByContentDescription((ViewGroup) child, contains);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static Button findButtonByText(ViewGroup root, String exact) {
        for (int i = 0; i < root.getChildCount(); i++) {
            View child = root.getChildAt(i);
            if (child instanceof Button) {
                CharSequence text = ((Button) child).getText();
                if (text != null && exact.contentEquals(text)) return (Button) child;
            }
            if (child instanceof ViewGroup) {
                Button found = findButtonByText((ViewGroup) child, exact);
                if (found != null) return found;
            }
        }
        return null;
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
