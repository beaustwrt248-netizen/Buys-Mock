package com.buysloans.nova;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

/**
 * Applies Nova's approved responsive chat composition after MainActivity builds its programmatic
 * view tree. The safety net intentionally does layout only; auth, Guardian and protected-action
 * authority remain in MainActivity/NovaApiClient.
 */
public final class NovaApplication extends Application {
    private final WeakHashMap<Activity, ViewTreeObserver.OnGlobalLayoutListener> watchers = new WeakHashMap<>();
    private final WeakHashMap<Activity, Long> lastPass = new WeakHashMap<>();
    private final WeakHashMap<View, View.OnLayoutChangeListener> conversationListeners = new WeakHashMap<>();

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override public void onActivityCreated(Activity activity, Bundle state) {}
            @Override public void onActivityStarted(Activity activity) {}
            @Override public void onActivityPaused(Activity activity) {}
            @Override public void onActivityStopped(Activity activity) {}
            @Override public void onActivitySaveInstanceState(Activity activity, Bundle state) {}

            @Override
            public void onActivityResumed(Activity activity) {
                if (!(activity instanceof MainActivity)) return;
                installWatcher(activity);
                activity.getWindow().getDecorView().post(() -> applyApprovedChatLayout(activity));
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
                ViewTreeObserver.OnGlobalLayoutListener listener = watchers.remove(activity);
                View decor = activity.getWindow().getDecorView();
                if (listener != null && decor.getViewTreeObserver().isAlive()) {
                    decor.getViewTreeObserver().removeOnGlobalLayoutListener(listener);
                }
                lastPass.remove(activity);
            }
        });
    }

    private void installWatcher(Activity activity) {
        if (watchers.containsKey(activity)) return;
        View decor = activity.getWindow().getDecorView();
        ViewTreeObserver.OnGlobalLayoutListener listener = () -> {
            long now = android.os.SystemClock.uptimeMillis();
            Long previous = lastPass.get(activity);
            if (previous != null && now - previous < 180L) return;
            lastPass.put(activity, now);
            decor.post(() -> applyApprovedChatLayout(activity));
        };
        watchers.put(activity, listener);
        decor.getViewTreeObserver().addOnGlobalLayoutListener(listener);
    }

    private void applyApprovedChatLayout(Activity activity) {
        View decor = activity.getWindow().getDecorView();
        EditText composerInput = findEditTextByHint(decor, "Message Nova…");
        if (composerInput == null) return;

        ViewParentChain chain = composerChain(composerInput);
        if (chain == null) return;

        fixOuterShell(activity, chain.chatCard);
        fixConversationScroll(activity, chain.chatCard);
        fixQuickActions(activity, decor);
        fixHero(activity, decor);
        fixMessageVisuals(activity, chain.chatCard);
        fixSuggestionChips(activity, chain.chatCard);
        fixComposer(activity, composerInput, chain.chatCard);
        fixFooterAndNavigation(activity, decor);
    }

    private void fixOuterShell(Activity activity, LinearLayout chatCard) {
        ScrollView outer = findAncestorScrollView(chatCard);
        if (outer == null) return;
        outer.setVerticalScrollBarEnabled(false);
        outer.setScrollbarFadingEnabled(true);
        outer.setFillViewport(true);
        outer.setClipToPadding(false);
    }

    private void fixConversationScroll(Activity activity, LinearLayout chatCard) {
        if (chatCard.getChildCount() == 0) return;

        ScrollView existing = null;
        LinearLayout conversation = null;
        View first = chatCard.getChildAt(0);
        if (first instanceof ScrollView) {
            existing = (ScrollView) first;
            if (existing.getChildCount() > 0 && existing.getChildAt(0) instanceof LinearLayout) {
                conversation = (LinearLayout) existing.getChildAt(0);
            }
        } else if (first instanceof LinearLayout) {
            conversation = (LinearLayout) first;
        }
        if (conversation == null) return;

        ScrollView chatScroll = existing;
        if (chatScroll == null) {
            chatCard.removeViewAt(0);
            chatScroll = new ScrollView(activity);
            chatScroll.setFillViewport(false);
            chatScroll.setClipToPadding(false);
            chatScroll.addView(conversation, new ScrollView.LayoutParams(
                    ScrollView.LayoutParams.MATCH_PARENT,
                    ScrollView.LayoutParams.WRAP_CONTENT));
            chatCard.addView(chatScroll, 0, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(activity, 150)));
        }

        chatScroll.setPadding(0, 0, dp(activity, 8), 0);
        chatScroll.setVerticalScrollBarEnabled(true);
        chatScroll.setScrollbarFadingEnabled(false);
        chatScroll.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);
        chatScroll.setVerticalScrollbarPosition(View.SCROLLBAR_POSITION_RIGHT);
        chatScroll.setNestedScrollingEnabled(true);
        chatScroll.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);

        final ScrollView finalScroll = chatScroll;
        final LinearLayout finalConversation = conversation;
        final ScrollView outer = findAncestorScrollView(chatCard);

        View.OnLayoutChangeListener previous = conversationListeners.remove(finalConversation);
        if (previous != null) finalConversation.removeOnLayoutChangeListener(previous);

        View.OnLayoutChangeListener listener = (v, l, t, r, b, ol, ot, or, ob) -> {
            if (b == ob && r == or) return;
            resizeConversationViewport(activity, finalScroll, finalConversation);
            finalScroll.post(() -> finalScroll.fullScroll(View.FOCUS_DOWN));
            if (outer != null && outer != finalScroll) {
                outer.postDelayed(() -> outer.scrollTo(0, 0), 30L);
            }
        };
        conversationListeners.put(finalConversation, listener);
        finalConversation.addOnLayoutChangeListener(listener);
        resizeConversationViewport(activity, finalScroll, finalConversation);
    }

    private void resizeConversationViewport(Activity activity, ScrollView chatScroll, LinearLayout conversation) {
        int screenHeightPx = activity.getResources().getDisplayMetrics().heightPixels;
        int maxPx = Math.min(dp(activity, 265), Math.round(screenHeightPx * .25f));
        int minPx = Math.min(dp(activity, 118), maxPx);
        int measured = conversation.getMeasuredHeight();
        if (measured <= 0) measured = minPx;
        int desired = Math.max(minPx, Math.min(maxPx, measured + dp(activity, 6)));

        ViewGroup.LayoutParams raw = chatScroll.getLayoutParams();
        if (raw instanceof LinearLayout.LayoutParams && raw.height != desired) {
            raw.height = desired;
            chatScroll.setLayoutParams(raw);
        }
    }

    private void fixQuickActions(Activity activity, View root) {
        List<HorizontalScrollView> strips = new ArrayList<>();
        collect(root, HorizontalScrollView.class, strips);
        for (HorizontalScrollView strip : strips) {
            if (strip.getChildCount() != 1 || !(strip.getChildAt(0) instanceof LinearLayout)) continue;
            LinearLayout row = (LinearLayout) strip.getChildAt(0);
            if (row.getChildCount() != 4) continue;

            strip.setFillViewport(true);
            strip.setHorizontalScrollBarEnabled(false);
            strip.setOverScrollMode(View.OVER_SCROLL_NEVER);
            row.setPadding(0, dp(activity, 3), 0, dp(activity, 1));

            strip.post(() -> {
                int width = strip.getWidth();
                if (width <= 0) return;
                int gap = dp(activity, 4);
                int cardWidth = Math.max(1, (width - gap * 3) / 4);
                row.setLayoutParams(new FrameLayout.LayoutParams(width, FrameLayout.LayoutParams.WRAP_CONTENT));
                for (int i = 0; i < row.getChildCount(); i++) {
                    View child = row.getChildAt(i);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(cardWidth, dp(activity, 92));
                    if (i > 0) lp.setMarginStart(gap);
                    child.setLayoutParams(lp);
                    if (child instanceof Button) {
                        Button button = (Button) child;
                        compactQuickActionLabel(button);
                        button.setTextSize(9.6f);
                        button.setMinWidth(0);
                        button.setMinHeight(0);
                        button.setPadding(dp(activity, 3), dp(activity, 4), dp(activity, 3), dp(activity, 4));
                        button.setIncludeFontPadding(false);
                    }
                }
                strip.scrollTo(0, 0);
            });
        }
    }

    private static void compactQuickActionLabel(Button button) {
        String value = String.valueOf(button.getText());
        if (value.contains("Daily Brief")) {
            button.setText("✧\nDaily Brief\nToday at a glance");
        } else if (value.contains("App Status")) {
            button.setText("▥\nApp Status\nCheck everything");
        } else if (value.contains("Find Devices")) {
            button.setText("▣\nFind Devices\nSearch catalogue");
        } else if (value.contains("Ask Anything")) {
            button.setText("◉\nAsk Anything\nI'll figure it out");
        }
    }

    private void fixHero(Activity activity, View root) {
        TextView brand = findText(root, "NOVA AI");
        if (brand == null || !(brand.getParent() instanceof LinearLayout)) return;
        LinearLayout brandCol = (LinearLayout) brand.getParent();
        if (!(brandCol.getParent() instanceof LinearLayout)) return;
        LinearLayout hero = (LinearLayout) brandCol.getParent();
        if (hero.getChildCount() != 3) return;

        hero.setGravity(android.view.Gravity.TOP);
        hero.setPadding(dp(activity, 1), 0, dp(activity, 1), 0);

        brand.setSingleLine(true);
        brand.setMaxLines(1);
        brand.setTextSize(17.5f);
        brand.setLetterSpacing(.15f);
        brand.setIncludeFontPadding(false);

        List<TextView> brandTexts = new ArrayList<>();
        collect(brandCol, TextView.class, brandTexts);
        for (TextView tv : brandTexts) {
            if (tv != brand) {
                tv.setTextSize(11f);
                tv.setIncludeFontPadding(false);
            }
        }

        View brainCol = hero.getChildAt(1);
        View toolsCol = hero.getChildAt(2);
        brandCol.setLayoutParams(weighted(.93f));
        brainCol.setLayoutParams(weighted(1.18f));
        toolsCol.setLayoutParams(weighted(.89f));

        View brain = findBySimpleClassName(brainCol, "NovaBrainView");
        if (brain != null) {
            brain.setMinimumWidth(0);
            brain.setMinimumHeight(0);
            brain.setLayoutParams(new LinearLayout.LayoutParams(dp(activity, 112), dp(activity, 77)));
        }

        List<TextView> brainTexts = new ArrayList<>();
        collect(brainCol, TextView.class, brainTexts);
        for (TextView tv : brainTexts) {
            String value = String.valueOf(tv.getText());
            tv.setIncludeFontPadding(false);
            if (value.contains("Ready") || value.contains("Thinking")) {
                tv.setTextSize(10.5f);
                tv.setPadding(0, 0, 0, 0);
            } else if (value.contains("●")) {
                tv.setTextSize(8f);
                tv.setPadding(0, 0, 0, 0);
            }
        }

        if (toolsCol instanceof LinearLayout) {
            LinearLayout tools = (LinearLayout) toolsCol;
            tools.setPadding(0, 0, 0, 0);
            if (tools.getChildCount() > 0 && tools.getChildAt(0) instanceof LinearLayout) {
                LinearLayout toolRow = (LinearLayout) tools.getChildAt(0);
                for (int i = 0; i < toolRow.getChildCount(); i++) {
                    View tool = toolRow.getChildAt(i);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(activity, 32), dp(activity, 32));
                    if (i > 0) lp.setMarginStart(dp(activity, 3));
                    tool.setLayoutParams(lp);
                    if (tool instanceof TextView) {
                        ((TextView) tool).setTextSize(14f);
                        ((TextView) tool).setIncludeFontPadding(false);
                    }
                }
            }
            List<TextView> statusTexts = new ArrayList<>();
            collect(tools, TextView.class, statusTexts);
            for (TextView tv : statusTexts) {
                String value = String.valueOf(tv.getText());
                if (value.contains("Online") || value.startsWith("v")) {
                    tv.setTextSize(10f);
                    tv.setIncludeFontPadding(false);
                }
            }
        }
    }

    private void fixMessageVisuals(Activity activity, LinearLayout chatCard) {
        ScrollView chatScroll = chatCard.getChildCount() > 0 && chatCard.getChildAt(0) instanceof ScrollView
                ? (ScrollView) chatCard.getChildAt(0) : null;
        if (chatScroll == null || chatScroll.getChildCount() == 0
                || !(chatScroll.getChildAt(0) instanceof LinearLayout)) return;
        LinearLayout conversation = (LinearLayout) chatScroll.getChildAt(0);

        for (int i = 0; i < conversation.getChildCount(); i++) {
            View item = conversation.getChildAt(i);
            if (!(item instanceof LinearLayout)) continue;
            LinearLayout row = (LinearLayout) item;
            for (int j = 0; j < row.getChildCount(); j++) {
                View child = row.getChildAt(j);
                if (child instanceof TextView) {
                    TextView tv = (TextView) child;
                    if ("◉".contentEquals(tv.getText())) tv.setText("♙");
                }
                if (child instanceof LinearLayout) {
                    LinearLayout bubble = (LinearLayout) child;
                    bubble.setPadding(dp(activity, 11), dp(activity, 8), dp(activity, 11), dp(activity, 8));
                    ViewGroup.LayoutParams raw = bubble.getLayoutParams();
                    if (raw instanceof LinearLayout.LayoutParams) {
                        ((LinearLayout.LayoutParams) raw).setMarginEnd(dp(activity, 6));
                        bubble.setLayoutParams(raw);
                    }
                    for (int k = 0; k < bubble.getChildCount(); k++) {
                        View bubbleChild = bubble.getChildAt(k);
                        if (bubbleChild instanceof TextView) {
                            TextView body = (TextView) bubbleChild;
                            body.setIncludeFontPadding(false);
                            CharSequence value = body.getText();
                            if (value != null && value.length() > 20) {
                                body.setTextSize(13f);
                                body.setLineSpacing(0, 1.04f);
                            }
                        }
                    }
                }
            }
        }
    }

    private void fixSuggestionChips(Activity activity, LinearLayout chatCard) {
        List<Button> buttons = new ArrayList<>();
        collect(chatCard, Button.class, buttons);
        for (Button button : buttons) {
            String value = String.valueOf(button.getText());
            if (!isSuggestion(value)) continue;
            button.setTextSize(9.5f);
            button.setMinHeight(0);
            button.setPadding(dp(activity, 5), 0, dp(activity, 5), 0);
            ViewGroup.LayoutParams raw = button.getLayoutParams();
            if (raw instanceof LinearLayout.LayoutParams) {
                raw.height = dp(activity, 40);
                button.setLayoutParams(raw);
            }
        }
    }

    private static boolean isSuggestion(String value) {
        return value.equals("What needs attention?")
                || value.equals("Show me the daily brief")
                || value.equals("Check app health")
                || value.equals("Find catalogue gaps")
                || value.equals("What’s new in the release?")
                || value.equals("▦  More");
    }

    private void fixComposer(Activity activity, EditText composerInput, LinearLayout chatCard) {
        chatCard.setPadding(dp(activity, 9), dp(activity, 9), dp(activity, 9), dp(activity, 9));
        ViewGroup.LayoutParams cardRaw = chatCard.getLayoutParams();
        if (cardRaw instanceof LinearLayout.LayoutParams) {
            ((LinearLayout.LayoutParams) cardRaw).bottomMargin = dp(activity, 12);
            chatCard.setLayoutParams(cardRaw);
        }

        if (!(composerInput.getParent() instanceof LinearLayout)) return;
        LinearLayout composer = (LinearLayout) composerInput.getParent();
        ViewGroup.LayoutParams raw = composer.getLayoutParams();
        if (raw instanceof LinearLayout.LayoutParams) {
            ((LinearLayout.LayoutParams) raw).topMargin = dp(activity, 7);
            composer.setLayoutParams(raw);
        }
        composerInput.setTextSize(13.5f);
        composerInput.setPadding(dp(activity, 7), dp(activity, 7), dp(activity, 7), dp(activity, 7));
        composerInput.setIncludeFontPadding(false);
        for (int i = 0; i < composer.getChildCount(); i++) {
            View child = composer.getChildAt(i);
            if (child == composerInput) continue;
            ViewGroup.LayoutParams lp = child.getLayoutParams();
            if (lp instanceof LinearLayout.LayoutParams) {
                if (i == composer.getChildCount() - 1) {
                    lp.width = dp(activity, 44);
                    lp.height = dp(activity, 44);
                } else {
                    lp.width = dp(activity, 36);
                    lp.height = dp(activity, 40);
                }
                child.setLayoutParams(lp);
            }
        }
    }

    private void fixFooterAndNavigation(Activity activity, View root) {
        TextView footer = findTextPrefix(root, "© 2026 Morley Buys");
        if (footer != null) {
            footer.setTextSize(9f);
            footer.setSingleLine(true);
            footer.setIncludeFontPadding(false);
            if (footer.getParent() instanceof LinearLayout) {
                ((LinearLayout) footer.getParent()).setPadding(0, dp(activity, 10), 0, dp(activity, 5));
            }
        }
        TextView secure = findTextPrefix(root, "◈  Secure");
        if (secure != null) {
            secure.setTextSize(9f);
            secure.setPadding(dp(activity, 4), 0, 0, 0);
            secure.setIncludeFontPadding(false);
        }

        List<Button> buttons = new ArrayList<>();
        collect(root, Button.class, buttons);
        LinearLayout nav = null;
        for (Button button : buttons) {
            String value = String.valueOf(button.getText());
            if (value.endsWith("\nChat") || value.endsWith("\nIntelligence")
                    || value.endsWith("\nUpdates") || value.endsWith("\nAccount")) {
                button.setTextSize(9.5f);
                button.setMinHeight(0);
                button.setPadding(dp(activity, 2), dp(activity, 3), dp(activity, 2), dp(activity, 3));
                button.setIncludeFontPadding(false);
                ViewGroup.LayoutParams raw = button.getLayoutParams();
                if (raw instanceof LinearLayout.LayoutParams) {
                    raw.height = dp(activity, 50);
                    button.setLayoutParams(raw);
                }
                if (button.getParent() instanceof LinearLayout) nav = (LinearLayout) button.getParent();
            }
        }
        if (nav != null) {
            nav.setPadding(dp(activity, 7), dp(activity, 4), dp(activity, 7), dp(activity, 4));
        }
    }

    private static LinearLayout.LayoutParams weighted(float weight) {
        return new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight);
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }

    private static ScrollView findAncestorScrollView(View view) {
        android.view.ViewParent parent = view.getParent();
        while (parent instanceof View) {
            if (parent instanceof ScrollView) return (ScrollView) parent;
            parent = parent.getParent();
        }
        return null;
    }

    private static EditText findEditTextByHint(View root, String hint) {
        if (root instanceof EditText && hint.contentEquals(((EditText) root).getHint())) return (EditText) root;
        if (!(root instanceof ViewGroup)) return null;
        ViewGroup group = (ViewGroup) root;
        for (int i = 0; i < group.getChildCount(); i++) {
            EditText found = findEditTextByHint(group.getChildAt(i), hint);
            if (found != null) return found;
        }
        return null;
    }

    private static TextView findText(View root, String text) {
        if (root instanceof TextView && text.contentEquals(((TextView) root).getText())) return (TextView) root;
        if (!(root instanceof ViewGroup)) return null;
        ViewGroup group = (ViewGroup) root;
        for (int i = 0; i < group.getChildCount(); i++) {
            TextView found = findText(group.getChildAt(i), text);
            if (found != null) return found;
        }
        return null;
    }

    private static TextView findTextPrefix(View root, String prefix) {
        if (root instanceof TextView) {
            CharSequence value = ((TextView) root).getText();
            if (value != null && value.toString().startsWith(prefix)) return (TextView) root;
        }
        if (!(root instanceof ViewGroup)) return null;
        ViewGroup group = (ViewGroup) root;
        for (int i = 0; i < group.getChildCount(); i++) {
            TextView found = findTextPrefix(group.getChildAt(i), prefix);
            if (found != null) return found;
        }
        return null;
    }

    private static View findBySimpleClassName(View root, String name) {
        if (root.getClass().getSimpleName().equals(name)) return root;
        if (!(root instanceof ViewGroup)) return null;
        ViewGroup group = (ViewGroup) root;
        for (int i = 0; i < group.getChildCount(); i++) {
            View found = findBySimpleClassName(group.getChildAt(i), name);
            if (found != null) return found;
        }
        return null;
    }

    private static <T extends View> void collect(View root, Class<T> type, List<T> out) {
        if (type.isInstance(root)) out.add(type.cast(root));
        if (!(root instanceof ViewGroup)) return;
        ViewGroup group = (ViewGroup) root;
        for (int i = 0; i < group.getChildCount(); i++) collect(group.getChildAt(i), type, out);
    }

    private static ViewParentChain composerChain(EditText input) {
        if (!(input.getParent() instanceof LinearLayout)) return null;
        LinearLayout composer = (LinearLayout) input.getParent();
        if (!(composer.getParent() instanceof LinearLayout)) return null;
        return new ViewParentChain((LinearLayout) composer.getParent());
    }

    private static final class ViewParentChain {
        final LinearLayout chatCard;
        ViewParentChain(LinearLayout chatCard) { this.chatCard = chatCard; }
    }
}
