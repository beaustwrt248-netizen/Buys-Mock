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
 * Applies the approved Nova chat composition after MainActivity builds its programmatic view tree.
 *
 * The chat screen intentionally keeps the hero, quick actions, suggestions, composer and bottom
 * navigation stable while only the conversation history scrolls. This also makes the four quick
 * actions fit the viewport instead of becoming a horizontally clipped carousel.
 */
public final class NovaApplication extends Application {
    private final WeakHashMap<Activity, ViewTreeObserver.OnGlobalLayoutListener> watchers = new WeakHashMap<>();
    private final WeakHashMap<Activity, Long> lastPass = new WeakHashMap<>();

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

        fixConversationScroll(activity, chain.chatCard);
        fixQuickActions(activity, decor);
        fixHero(activity, decor);
        fixMessageVisuals(activity, chain.chatCard);
        fixFooterAndNavigation(activity, decor);
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

        final ScrollView outer = findAncestorScrollView(chatCard);
        int screenHeightDp = Math.round(activity.getResources().getDisplayMetrics().heightPixels
                / activity.getResources().getDisplayMetrics().density);
        int conversationHeightDp = Math.max(300, Math.min(430, Math.round(screenHeightDp * .38f)));

        ScrollView chatScroll = existing;
        if (chatScroll == null) {
            chatCard.removeViewAt(0);
            chatScroll = new ScrollView(activity);
            chatScroll.setFillViewport(false);
            chatScroll.setClipToPadding(false);
            chatScroll.setPadding(0, 0, dp(activity, 2), 0);
            chatScroll.addView(conversation, new ScrollView.LayoutParams(
                    ScrollView.LayoutParams.MATCH_PARENT,
                    ScrollView.LayoutParams.WRAP_CONTENT));
            chatCard.addView(chatScroll, 0, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(activity, conversationHeightDp)));
            final ScrollView finalScroll = chatScroll;
            conversation.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
                if (b == ob) return;
                finalScroll.post(() -> finalScroll.fullScroll(View.FOCUS_DOWN));
                if (outer != null && outer != finalScroll) {
                    outer.postDelayed(() -> outer.scrollTo(0, 0), 40L);
                }
            });
        } else {
            ViewGroup.LayoutParams raw = chatScroll.getLayoutParams();
            if (raw instanceof LinearLayout.LayoutParams) {
                raw.height = dp(activity, conversationHeightDp);
                chatScroll.setLayoutParams(raw);
            }
        }

        chatScroll.setVerticalScrollBarEnabled(true);
        chatScroll.setScrollbarFadingEnabled(false);
        chatScroll.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);
        chatScroll.setNestedScrollingEnabled(true);
        chatScroll.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);

        if (outer != null && outer != chatScroll) {
            outer.setVerticalScrollBarEnabled(false);
            outer.setScrollbarFadingEnabled(true);
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
            row.setLayoutParams(new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT));
            row.setPadding(0, dp(activity, 8), 0, dp(activity, 4));

            for (int i = 0; i < row.getChildCount(); i++) {
                View child = row.getChildAt(i);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(activity, 116), 1f);
                if (i > 0) lp.setMarginStart(dp(activity, 3));
                if (i < row.getChildCount() - 1) lp.setMarginEnd(dp(activity, 3));
                child.setLayoutParams(lp);
                if (child instanceof Button) {
                    Button button = (Button) child;
                    button.setTextSize(11.5f);
                    button.setPadding(dp(activity, 5), dp(activity, 7), dp(activity, 5), dp(activity, 7));
                }
            }
            strip.scrollTo(0, 0);
        }
    }

    private void fixHero(Activity activity, View root) {
        TextView brand = findText(root, "NOVA AI");
        if (brand == null || !(brand.getParent() instanceof LinearLayout)) return;
        LinearLayout brandCol = (LinearLayout) brand.getParent();
        if (!(brandCol.getParent() instanceof LinearLayout)) return;
        LinearLayout hero = (LinearLayout) brandCol.getParent();
        if (hero.getChildCount() != 3) return;

        brand.setSingleLine(true);
        brand.setMaxLines(1);
        brand.setTextSize(19f);
        brand.setLetterSpacing(.16f);

        View brainCol = hero.getChildAt(1);
        View toolsCol = hero.getChildAt(2);
        brandCol.setLayoutParams(weighted(.98f));
        brainCol.setLayoutParams(weighted(1.14f));
        toolsCol.setLayoutParams(weighted(1.02f));

        View brain = findBySimpleClassName(brainCol, "NovaBrainView");
        if (brain != null) {
            brain.setLayoutParams(new LinearLayout.LayoutParams(dp(activity, 142), dp(activity, 120)));
        }

        if (toolsCol instanceof LinearLayout) {
            LinearLayout tools = (LinearLayout) toolsCol;
            if (tools.getChildCount() > 0 && tools.getChildAt(0) instanceof LinearLayout) {
                LinearLayout toolRow = (LinearLayout) tools.getChildAt(0);
                for (int i = 0; i < toolRow.getChildCount(); i++) {
                    View tool = toolRow.getChildAt(i);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(activity, 38), dp(activity, 38));
                    if (i > 0) lp.setMarginStart(dp(activity, 3));
                    tool.setLayoutParams(lp);
                    if (tool instanceof TextView) ((TextView) tool).setTextSize(16f);
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
                    for (int k = 0; k < bubble.getChildCount(); k++) {
                        View bubbleChild = bubble.getChildAt(k);
                        if (bubbleChild instanceof TextView) {
                            TextView body = (TextView) bubbleChild;
                            CharSequence value = body.getText();
                            if (value != null && value.length() > 20) {
                                body.setTextSize(14.5f);
                                body.setLineSpacing(0, 1.10f);
                            }
                        }
                    }
                }
            }
        }
    }

    private void fixFooterAndNavigation(Activity activity, View root) {
        TextView footer = findTextPrefix(root, "© 2026 Morley Buys");
        if (footer != null) {
            footer.setTextSize(10.5f);
            footer.setSingleLine(true);
        }
        TextView secure = findTextPrefix(root, "◈  Secure");
        if (secure != null) {
            secure.setTextSize(10f);
            secure.setPadding(dp(activity, 6), 0, 0, 0);
        }

        List<Button> buttons = new ArrayList<>();
        collect(root, Button.class, buttons);
        for (Button button : buttons) {
            String value = String.valueOf(button.getText());
            if (value.endsWith("\nChat") || value.endsWith("\nIntelligence")
                    || value.endsWith("\nUpdates") || value.endsWith("\nAccount")) {
                button.setTextSize(11f);
                button.setMinHeight(dp(activity, 62));
                button.setPadding(dp(activity, 3), dp(activity, 6), dp(activity, 3), dp(activity, 6));
            }
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
