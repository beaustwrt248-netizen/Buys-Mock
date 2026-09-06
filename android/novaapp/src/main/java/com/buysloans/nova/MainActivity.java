package com.buysloans.nova;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity implements UpdateManager.Listener {
    private enum Tab { CHAT, INTELLIGENCE, UPDATES, ACCOUNT }
    private static final String CLEAR_CONVERSATION = "Clear conversation";

    private static final class ChatLine {
        final String who, value, time;
        final boolean user;
        ChatLine(String who, String value, boolean user) {
            this(who, value, user, new SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date()));
        }
        ChatLine(String who, String value, boolean user, String time) {
            this.who = who;
            this.value = value;
            this.user = user;
            this.time = time;
        }
    }

    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final NovaApiClient api = new NovaApiClient();
    private final NovaAssistantEngine assistant = new NovaAssistantEngine(api);
    private final List<ChatLine> chatHistory = new ArrayList<>();

    private LinearLayout root, conversation, shell;
    private ScrollView scroll;
    private TextView updateStatus, intelligenceResult, brainStatus;
    private Button updateButton;
    private EditText chatInput;
    private NovaBrainView brainView;
    private UpdateManager updateManager;
    private UpdateManager.UpdateInfo pendingUpdate;
    private CaptchaChallenge captchaChallenge;
    private Tab activeTab = Tab.CHAT;

    private int background, surface, surfaceRaised, primary, secondary, accent, accentStrong, violet,
            pink, success, warning, danger, outline, outlineBlue;

    private static int dp(Activity a, int v) {
        return Math.round(v * a.getResources().getDisplayMetrics().density);
    }

    private TextView text(String value, float size, int color) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(size);
        v.setTextColor(color);
        v.setLineSpacing(0, 1.08f);
        return v;
    }

    private GradientDrawable rounded(int fill, int radius, int strokeColor) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(this, radius));
        if (strokeColor != Color.TRANSPARENT) d.setStroke(dp(this, 1), strokeColor);
        return d;
    }

    private GradientDrawable gradient(int radius, int strokeColor, int... colors) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR, colors);
        d.setCornerRadius(dp(this, radius));
        if (strokeColor != Color.TRANSPARENT) d.setStroke(dp(this, 1), strokeColor);
        return d;
    }

    private Button button(String label) {
        return button(label, false);
    }

    private Button button(String label, boolean primaryAction) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(14);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setTextColor(primary);
        b.setMinHeight(dp(this, 48));
        b.setPadding(dp(this, 14), dp(this, 10), dp(this, 14), dp(this, 10));
        b.setBackground(primaryAction
                ? gradient(16, accent, Color.rgb(61, 100, 224), Color.rgb(75, 111, 227))
                : rounded(surfaceRaised, 16, outline));
        return b;
    }

    private Button navButton(String label, Tab tab) {
        boolean selected = tab == activeTab;
        Button b = button(navVisual(label), selected);
        b.setTextSize(11);
        b.setGravity(Gravity.CENTER);
        b.setMinHeight(dp(this, 64));
        b.setPadding(dp(this, 4), dp(this, 7), dp(this, 4), dp(this, 7));
        b.setBackground(selected
                ? gradient(18, accent, Color.rgb(63, 103, 231), Color.rgb(75, 112, 224))
                : rounded(Color.rgb(13, 29, 50), 18, outline));
        b.setOnClickListener(v -> {
            activeTab = tab;
            showWorkspace();
        });
        return b;
    }

    private String navVisual(String label) {
        if ("Chat".equals(label)) return "▱\nChat";
        if ("Intelligence".equals(label)) return "▥\nIntelligence";
        if ("Updates".equals(label)) return (pendingUpdate == null ? "⇩" : "●") + "\nUpdates";
        return "♙\nAccount";
    }

    private void add(View v, int top) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(this, top);
        root.addView(v, p);
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        background = Color.rgb(2, 10, 22);
        surface = Color.rgb(7, 23, 42);
        surfaceRaised = Color.rgb(12, 34, 59);
        primary = Color.rgb(239, 244, 255);
        secondary = Color.rgb(169, 185, 211);
        accent = Color.rgb(82, 139, 255);
        accentStrong = Color.rgb(65, 98, 224);
        violet = Color.rgb(143, 91, 255);
        pink = Color.rgb(247, 75, 170);
        success = Color.rgb(77, 226, 157);
        warning = Color.rgb(255, 193, 61);
        danger = Color.rgb(255, 103, 122);
        outline = Color.rgb(38, 66, 101);
        outlineBlue = Color.rgb(23, 91, 166);
        updateManager = new UpdateManager(this, this);
        showLogin();
        updateManager.checkForUpdates();
    }

    private void base(String subtitle, boolean withNav) {
        if (captchaChallenge != null) {
            captchaChallenge.destroy();
            captchaChallenge = null;
        }

        shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setBackgroundColor(background);

        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(background);
        scroll.setClipToPadding(false);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(this, 18), dp(this, withNav ? 16 : 28), dp(this, 18), dp(this, 22));
        scroll.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));

        shell.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        if (!withNav) {
            LinearLayout brandRow = new LinearLayout(this);
            brandRow.setOrientation(LinearLayout.HORIZONTAL);
            brandRow.setGravity(Gravity.CENTER_VERTICAL);
            TextView brand = text("NOVA AI", 17, accent);
            brand.setLetterSpacing(.22f);
            brand.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            TextView version = text("v" + BuildConfig.VERSION_NAME, 12, secondary);
            version.setGravity(Gravity.END);
            brandRow.addView(brand, new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            brandRow.addView(version);
            root.addView(brandRow);

            TextView title = text("Your AI Assistant\nfor Morley", 24, primary);
            title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            title.setPadding(0, dp(this, 14), 0, dp(this, 5));
            root.addView(title);

            TextView sub = text(subtitle, 14, secondary);
            sub.setPadding(0, 0, 0, dp(this, 18));
            root.addView(sub);
        }

        if (withNav) {
            LinearLayout nav = new LinearLayout(this);
            nav.setOrientation(LinearLayout.HORIZONTAL);
            nav.setPadding(dp(this, 8), dp(this, 10), dp(this, 8), dp(this, 10));
            nav.setBackgroundColor(Color.rgb(4, 18, 34));

            LinearLayout.LayoutParams each = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            each.setMarginEnd(dp(this, 3));
            LinearLayout.LayoutParams last = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);

            nav.addView(navButton("Chat",Tab.CHAT), each);
            nav.addView(navButton("Intelligence",Tab.INTELLIGENCE), each);
            nav.addView(navButton("Updates",Tab.UPDATES), each);
            nav.addView(navButton("Account",Tab.ACCOUNT), last);

            shell.addView(nav, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
        }

        setContentView(shell);
    }

    private EditText input(String hint, boolean password) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setHintTextColor(secondary);
        e.setTextColor(primary);
        e.setTextSize(15);
        e.setSingleLine(true);
        e.setPadding(dp(this, 14), dp(this, 12), dp(this, 14), dp(this, 12));
        e.setBackground(rounded(surfaceRaised, 16, outline));
        e.setInputType(password
                ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD
                : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        return e;
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(this, 16), dp(this, 16), dp(this, 16), dp(this, 16));
        c.setBackground(gradient(20, outlineBlue, Color.rgb(5, 22, 39), Color.rgb(6, 18, 34)));
        return c;
    }

    private void section(String title, String subtitle) {
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        TextView h = text(title, 20, primary);
        h.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        wrap.addView(h);
        if (subtitle != null && !subtitle.isBlank()) {
            TextView s = text(subtitle, 13, secondary);
            s.setPadding(0, dp(this, 3), 0, 0);
            wrap.addView(s);
        }
        add(wrap, 24);
    }

    private void watch(EditText input, Runnable changed) {
        input.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int count, int after) {}
            public void onTextChanged(CharSequence s, int st, int before, int count) { changed.run(); }
            public void afterTextChanged(Editable e) {}
        });
    }

    private void showLogin() {
        assistant.resetContext();
        chatHistory.clear();
        conversation = null;
        base("Secure access to Nova intelligence", false);

        LinearLayout c = card();
        add(c, 2);
        TextView heading = text("Sign in to Morley", 21, primary);
        heading.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        c.addView(heading);

        TextView note = text(
                "Use your authorised Morley Admin account. Nova can read approved intelligence; protected writes remain unavailable.",
                14, secondary);
        note.setPadding(0, dp(this, 7), 0, dp(this, 12));
        c.addView(note);

        EditText email = input("Email", false);
        EditText password = input("Password", true);
        c.addView(email);
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        pp.topMargin = dp(this, 8);
        c.addView(password, pp);

        TextView challengeStatus = text("Security check loading…", 13, secondary);
        challengeStatus.setPadding(0, dp(this, 10), 0, dp(this, 5));
        c.addView(challengeStatus);

        Button signIn = button("Complete security check", true);
        signIn.setEnabled(false);
        signIn.setAlpha(.55f);
        boolean[] captchaReady = {false};

        Runnable sync = () -> {
            boolean ready = captchaReady[0]
                    && email.getText().toString().trim().contains("@")
                    && !password.getText().toString().isBlank();
            signIn.setEnabled(ready);
            signIn.setAlpha(ready ? 1f : .55f);
            signIn.setText(captchaReady[0] ? "Sign in securely" : "Complete security check");
        };

        captchaChallenge = new CaptchaChallenge(this, (ready, message) -> {
            captchaReady[0] = ready;
            challengeStatus.setText(message);
            challengeStatus.setTextColor(ready ? success : secondary);
            sync.run();
        });
        c.addView(captchaChallenge.view());

        TextView status = text("", 13, secondary);
        status.setPadding(0, dp(this, 6), 0, dp(this, 4));
        c.addView(status);
        c.addView(signIn);
        watch(email, sync);
        watch(password, sync);

        signIn.setOnClickListener(v -> {
            String e = email.getText().toString().trim();
            String p = password.getText().toString();
            if (e.isEmpty() || p.isEmpty()) {
                status.setText("Enter your email and password.");
                status.setTextColor(danger);
                return;
            }
            if (captchaChallenge == null || !captchaChallenge.isReady()) {
                status.setText("Complete the security check first.");
                status.setTextColor(danger);
                return;
            }
            String token = captchaChallenge.token();
            signIn.setEnabled(false);
            signIn.setAlpha(.55f);
            status.setTextColor(secondary);
            status.setText("Verifying your authorised Morley account…");
            worker.execute(() -> {
                try {
                    api.signIn(e, p, token);
                    runOnUiThread(() -> {
                        activeTab = Tab.CHAT;
                        showWorkspace();
                    });
                } catch (Exception ex) {
                    runOnUiThread(() -> {
                        status.setText("Sign-in failed. Check your details and try again.");
                        status.setTextColor(danger);
                        captchaReady[0] = false;
                        if (captchaChallenge != null) captchaChallenge.reset();
                        sync.run();
                    });
                }
            });
        });

        addOtaSection();
        footer();
    }

    private void showWorkspace() {
        base(activeTab == Tab.CHAT ? "Conversation"
                : activeTab == Tab.INTELLIGENCE ? "Live Morley intelligence"
                : activeTab == Tab.UPDATES ? "Secure signed releases"
                : "Account & authority", true);

        if (activeTab == Tab.CHAT) showChatTab();
        else if (activeTab == Tab.INTELLIGENCE) showIntelligenceTab();
        else if (activeTab == Tab.UPDATES) showUpdatesTab();
        else showAccountTab();
    }

    private void showChatTab() {
        addHeroHeader();
        addQuickActionCards();

        LinearLayout chatCard = card();
        chatCard.setPadding(dp(this, 12), dp(this, 12), dp(this, 12), dp(this, 12));
        add(chatCard, 14);

        conversation = new LinearLayout(this);
        conversation.setOrientation(LinearLayout.VERTICAL);
        chatCard.addView(conversation);

        if (chatHistory.isEmpty()) {
            chatHistory.add(new ChatLine(
                    "Nova",
                    "Hi — I’m Nova. I can chat naturally, check Morley, research the web, analyse data, prepare fixes and more. If I don’t know something, I’ll search and find the answer for you with sources.\n\nHow can I help?",
                    false));
        }
        for (ChatLine line : chatHistory) addBubbleView(line);

        addSuggestionRows(chatCard);

        LinearLayout composer = new LinearLayout(this);
        composer.setOrientation(LinearLayout.HORIZONTAL);
        composer.setGravity(Gravity.CENTER_VERTICAL);
        composer.setPadding(dp(this, 4), dp(this, 4), dp(this, 4), dp(this, 4));
        composer.setBackground(rounded(Color.rgb(5, 26, 48), 28, outlineBlue));

        Button attach = compactIconButton("⌕");
        attach.setContentDescription("Attach");
        composer.addView(attach, new LinearLayout.LayoutParams(dp(this, 46), dp(this, 48)));

        chatInput = input("Message Nova…", false);
        chatInput.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        chatInput.setSingleLine(false);
        chatInput.setMinLines(1);
        chatInput.setMaxLines(5);
        chatInput.setBackgroundColor(Color.TRANSPARENT);
        composer.addView(chatInput, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        Button mic = compactIconButton("♩");
        mic.setContentDescription("Voice");
        composer.addView(mic, new LinearLayout.LayoutParams(dp(this, 46), dp(this, 48)));

        Button send = compactIconButton("➤");
        send.setTextSize(19);
        send.setBackground(gradient(24, accent, Color.rgb(62, 103, 235), Color.rgb(76, 112, 238)));
        LinearLayout.LayoutParams sendLp = new LinearLayout.LayoutParams(dp(this, 52), dp(this, 52));
        sendLp.setMarginStart(dp(this, 4));
        composer.addView(send, sendLp);

        LinearLayout.LayoutParams composerLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        composerLp.topMargin = dp(this, 14);
        chatCard.addView(composer, composerLp);

        send.setOnClickListener(v -> sendQuestion(chatInput, send));
        attach.setOnClickListener(v -> addChat("Nova",
                "Attachments are coming next. For now, tell me what you want me to inspect and I’ll work from the connected Morley sources.", false));
        mic.setOnClickListener(v -> addChat("Nova",
                "Voice input is next on the Nova roadmap. Type your request here and I’ll handle it the same way.", false));

        footer();
    }

    private void addHeroHeader() {
        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.HORIZONTAL);
        hero.setGravity(Gravity.TOP);
        hero.setPadding(dp(this, 2), dp(this, 4), dp(this, 2), dp(this, 6));

        LinearLayout brandCol = new LinearLayout(this);
        brandCol.setOrientation(LinearLayout.VERTICAL);
        TextView brand = text("NOVA AI", 22, accent);
        brand.setLetterSpacing(.20f);
        brand.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        brandCol.addView(brand);
        TextView subtitle = text("Your AI Assistant\nfor Morley", 13, secondary);
        subtitle.setPadding(0, dp(this, 6), 0, 0);
        brandCol.addView(subtitle);

        LinearLayout brainCol = new LinearLayout(this);
        brainCol.setOrientation(LinearLayout.VERTICAL);
        brainCol.setGravity(Gravity.CENTER_HORIZONTAL);
        brainView = new NovaBrainView(this);
        brainCol.addView(brainView, new LinearLayout.LayoutParams(dp(this, 132), dp(this, 112)));
        brainStatus = text("Ready", 13, primary);
        brainStatus.setGravity(Gravity.CENTER);
        brainStatus.setPadding(0, 0, 0, 0);
        brainCol.addView(brainStatus);
        TextView dots = text("●  ●  ●", 11, accent);
        dots.setGravity(Gravity.CENTER);
        dots.setTextColor(violet);
        brainCol.addView(dots);

        LinearLayout toolsCol = new LinearLayout(this);
        toolsCol.setOrientation(LinearLayout.VERTICAL);
        toolsCol.setGravity(Gravity.END);

        LinearLayout toolRow = new LinearLayout(this);
        toolRow.setOrientation(LinearLayout.HORIZONTAL);
        toolRow.setGravity(Gravity.END);

        Button search = circleTool("⌕");
        Button history = circleTool("↶");
        Button settings = circleTool("⚙");
        toolRow.addView(search);
        toolRow.addView(history);
        toolRow.addView(settings);
        toolsCol.addView(toolRow);

        TextView online = text("●  Online", 12, success);
        online.setGravity(Gravity.END);
        online.setPadding(0, dp(this, 9), 0, 0);
        toolsCol.addView(online);
        TextView version = text("v" + BuildConfig.VERSION_NAME, 12, secondary);
        version.setGravity(Gravity.END);
        toolsCol.addView(version);

        hero.addView(brandCol, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, .92f));
        hero.addView(brainCol, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.12f));
        hero.addView(toolsCol, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, .96f));
        root.addView(hero);

        search.setOnClickListener(v -> {
            if (chatInput != null) {
                chatInput.requestFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.showSoftInput(chatInput, InputMethodManager.SHOW_IMPLICIT);
            }
        });
        history.setOnClickListener(v -> {
            if (scroll != null) scroll.smoothScrollTo(0, 0);
        });
        history.setOnLongClickListener(v -> {
            assistant.resetContext();
            chatHistory.clear();
            if (conversation != null) {
                conversation.removeAllViews();
                addChat("Nova", "Conversation cleared. We can start fresh — what’s on your mind?", false);
            }
            return true;
        });
        history.setContentDescription(CLEAR_CONVERSATION);
        settings.setOnClickListener(v -> {
            activeTab = Tab.ACCOUNT;
            showWorkspace();
        });
    }

    private Button circleTool(String icon) {
        Button b = new Button(this);
        b.setText(icon);
        b.setTextColor(primary);
        b.setTextSize(17);
        b.setGravity(Gravity.CENTER);
        b.setMinWidth(0);
        b.setMinHeight(0);
        b.setPadding(0, 0, 0, 0);
        b.setBackground(rounded(Color.rgb(3, 18, 33), 24, outline));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(this, 42), dp(this, 42));
        p.setMarginStart(dp(this, 5));
        b.setLayoutParams(p);
        return b;
    }

    private Button compactIconButton(String icon) {
        Button b = new Button(this);
        b.setText(icon);
        b.setTextColor(primary);
        b.setTextSize(17);
        b.setGravity(Gravity.CENTER);
        b.setMinWidth(0);
        b.setMinHeight(0);
        b.setPadding(0, 0, 0, 0);
        b.setBackgroundColor(Color.TRANSPARENT);
        return b;
    }

    private void addQuickActionCards() {
        HorizontalScrollView strip = new HorizontalScrollView(this);
        strip.setHorizontalScrollBarEnabled(false);
        strip.setFillViewport(false);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(this, 8), 0, dp(this, 4));

        Button daily = quickCard("✧", "Daily Brief", "What’s happening\ntoday?");
        Button status = quickCard("▥", "App Status", "Check everything");
        Button devices = quickCard("▣", "Find Devices", "Search catalogue");
        Button anything = quickCard("◉", "Ask Anything", "I’ll figure it out");

        row.addView(daily, quickCardParams());
        row.addView(status, quickCardParams());
        row.addView(devices, quickCardParams());
        row.addView(anything, quickCardParams());

        strip.addView(row);
        add(strip, 6);

        daily.setOnClickListener(v -> submitQuick("Give me the daily brief"));
        status.setOnClickListener(v -> submitQuick("Check app health and what needs attention"));
        devices.setOnClickListener(v -> submitQuick("Find catalogue gaps and missing device information"));
        anything.setOnClickListener(v -> {
            if (chatInput != null) chatInput.requestFocus();
        });
    }

    private LinearLayout.LayoutParams quickCardParams() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(this, 132), dp(this, 116));
        p.setMarginEnd(dp(this, 8));
        return p;
    }

    private Button quickCard(String icon, String title, String subtitle) {
        Button b = new Button(this);
        b.setText(icon + "\n" + title + "\n" + subtitle);
        b.setAllCaps(false);
        b.setTextColor(primary);
        b.setTextSize(12);
        b.setGravity(Gravity.CENTER);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setMinWidth(0);
        b.setMinHeight(0);
        b.setPadding(dp(this, 10), dp(this, 8), dp(this, 10), dp(this, 8));
        b.setBackground(gradient(18, outlineBlue, Color.rgb(4, 25, 47), Color.rgb(5, 16, 31)));
        return b;
    }

    private void addSuggestionRows(LinearLayout host) {
        LinearLayout first = new LinearLayout(this);
        first.setOrientation(LinearLayout.HORIZONTAL);
        first.setGravity(Gravity.CENTER);
        first.setPadding(0, dp(this, 14), 0, 0);
        first.addView(chip("What needs attention?", "What needs my attention?"),
                new LinearLayout.LayoutParams(0, dp(this, 46), 1f));
        LinearLayout.LayoutParams mid = new LinearLayout.LayoutParams(0, dp(this, 46), 1f);
        mid.setMarginStart(dp(this, 6));
        first.addView(chip("Show me the daily brief", "Give me the daily brief"), mid);
        LinearLayout.LayoutParams right = new LinearLayout.LayoutParams(0, dp(this, 46), 1f);
        right.setMarginStart(dp(this, 6));
        first.addView(chip("Check app health", "Check app health"), right);
        host.addView(first);

        LinearLayout second = new LinearLayout(this);
        second.setOrientation(LinearLayout.HORIZONTAL);
        second.setGravity(Gravity.CENTER);
        second.setPadding(0, dp(this, 6), 0, 0);
        second.addView(chip("Find catalogue gaps", "Find catalogue gaps"),
                new LinearLayout.LayoutParams(0, dp(this, 46), 1f));
        LinearLayout.LayoutParams releaseLp = new LinearLayout.LayoutParams(0, dp(this, 46), 1.25f);
        releaseLp.setMarginStart(dp(this, 6));
        second.addView(chip("What’s new in the release?", "What's new in the release?"), releaseLp);
        LinearLayout.LayoutParams moreLp = new LinearLayout.LayoutParams(0, dp(this, 46), .65f);
        moreLp.setMarginStart(dp(this, 6));
        second.addView(chip("▦  More", "show me more"), moreLp);
        host.addView(second);
    }

    private Button chip(String visible, String query) {
        Button b = new Button(this);
        b.setText(visible);
        b.setAllCaps(false);
        b.setTextColor(primary);
        b.setTextSize(10);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(this, 7), 0, dp(this, 7), 0);
        b.setMinWidth(0);
        b.setMinHeight(0);
        b.setBackground(gradient(23, outlineBlue, Color.rgb(7, 30, 53), Color.rgb(11, 27, 47)));
        b.setOnClickListener(v -> submitQuick(query));
        return b;
    }

    private void submitQuick(String query) {
        if (conversation == null) return;
        if (chatInput == null) {
            addChat("You", query, true);
            worker.execute(() -> {
                try {
                    String reply = assistant.answer(query);
                    runOnUiThread(() -> addChat("Nova", reply, false));
                } catch (Exception e) {
                    runOnUiThread(() -> addChat("Nova",
                            "I understood the request, but the authorised source is unavailable right now.", false));
                }
            });
            return;
        }
        chatInput.setText(query);
        sendQuestion(chatInput, null);
    }

    private void showIntelligenceTab() {
        section("Intelligence", "Live Morley intelligence, Nova knowledge and verified learning.");
        LinearLayout statusCard = card();
        add(statusCard, 0);
        TextView online = text("●  Nova intelligence online", 18, success);
        online.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        statusCard.addView(online);
        TextView boundary = text(
                "Live authorised data, Nova knowledge and verified learning are connected. Protected approvals and repairs remain human-gated.",
                13, secondary);
        boundary.setPadding(0, dp(this, 8), 0, 0);
        statusCard.addView(boundary);

        section("Quick intelligence", "Choose a live view.");
        LinearLayout result = card();
        add(result, 8);
        intelligenceResult = text("Choose a live intelligence view below.", 14, secondary);
        result.addView(intelligenceResult);

        Button attention = button("What needs my attention?", true);
        attention.setOnClickListener(v -> runAttention(attention));
        add(attention, 10);

        addActionRow("Business overview", IntentRouter.Intent.PERFORMANCE,
                "Guardian", IntentRouter.Intent.GUARDIAN);
        addActionRow("Support", IntentRouter.Intent.SUPPORT,
                "Inventory", IntentRouter.Intent.INVENTORY);
        addActionRow("Catalogue", IntentRouter.Intent.CATALOGUE,
                "Learning", IntentRouter.Intent.LEARNING);

        Button releases = button("Release & feature state");
        releases.setOnClickListener(v -> runSummary(IntentRouter.Intent.RELEASES, releases));
        add(releases, 8);
        footer();
    }

    private void showUpdatesTab() {
        section("Updates", "Secure signed releases.");
        addOtaSection(false);
        footer();
    }

    private void showAccountTab() {
        section("Account", "Signed-in identity and authority.");
        LinearLayout account = card();
        add(account, 0);
        TextView online = text("●  Signed in", 18, success);
        online.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        account.addView(online);

        TextView email = text(api.signedInEmail(), 14, primary);
        email.setSingleLine(true);
        email.setEllipsize(TextUtils.TruncateAt.END);
        email.setPadding(0, dp(this, 7), 0, 0);
        account.addView(email);

        TextView boundary = text(
                "Nova can read authorised intelligence and knowledge. Protected approvals, pricing decisions, production releases and repairs remain human-gated.",
                13, secondary);
        boundary.setPadding(0, dp(this, 10), 0, 0);
        account.addView(boundary);

        Button logout = button("Sign out of Nova");
        logout.setOnClickListener(v -> {
            assistant.resetContext();
            chatHistory.clear();
            api.signOut();
            showLogin();
        });
        add(logout, 18);
        footer();
    }

    private void sendQuestion(EditText ask, Button send) {
        String q = ask.getText().toString().trim();
        if (q.isEmpty()) return;
        ask.setText("");
        addChat("You", q, true);
        setBrainThinking(true);
        if (send != null) setWorking(send, true, "…");

        worker.execute(() -> {
            try {
                String reply = assistant.answer(q);
                runOnUiThread(() -> {
                    addChat("Nova", reply, false);
                    setBrainThinking(false);
                    if (send != null) {
                        send.setEnabled(true);
                        send.setAlpha(1f);
                        send.setText("➤");
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    addChat("Nova",
                            "I understood the request, but the authorised source is unavailable right now. We can keep talking, and no protected change was made.",
                            false);
                    setBrainThinking(false);
                    if (send != null) {
                        send.setEnabled(true);
                        send.setAlpha(1f);
                        send.setText("➤");
                    }
                });
            }
        });
    }

    private void setBrainThinking(boolean thinking) {
        if (brainView != null) brainView.setThinking(thinking);
        if (brainStatus != null) {
            brainStatus.setText(thinking ? "Thinking…" : "Ready");
            brainStatus.setTextColor(thinking ? primary : secondary);
        }
    }

    private void addActionRow(String left, IntentRouter.Intent li, String right, IntentRouter.Intent ri) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        Button a = button(left), b = button(right);
        a.setOnClickListener(v -> runSummary(li, a));
        b.setOnClickListener(v -> runSummary(ri, b));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMarginEnd(dp(this, 4));
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        rp.setMarginStart(dp(this, 4));

        row.addView(a, lp);
        row.addView(b, rp);
        add(row, 8);
    }

    private void runSummary(IntentRouter.Intent intent, Button trigger) {
        if (intelligenceResult != null) {
            intelligenceResult.setText("Reading live Morley data…");
            intelligenceResult.setTextColor(secondary);
        }
        setWorking(trigger, true, "Loading…");
        worker.execute(() -> {
            try {
                String result = assistant.answer(promptFor(intent));
                runOnUiThread(() -> {
                    if (intelligenceResult != null) {
                        intelligenceResult.setText(result);
                        intelligenceResult.setTextColor(primary);
                    }
                    setWorking(trigger, false, labelFor(intent));
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (intelligenceResult != null) {
                        intelligenceResult.setText("That authorised live source is unavailable right now.");
                        intelligenceResult.setTextColor(danger);
                    }
                    setWorking(trigger, false, labelFor(intent));
                });
            }
        });
    }

    private void runAttention(Button trigger) {
        if (intelligenceResult != null) {
            intelligenceResult.setText("Checking the highest-priority live signals…");
            intelligenceResult.setTextColor(secondary);
        }
        setWorking(trigger, true, "Checking attention…");
        worker.execute(() -> {
            try {
                String result = assistant.attention();
                runOnUiThread(() -> {
                    if (intelligenceResult != null) {
                        intelligenceResult.setText(result);
                        intelligenceResult.setTextColor(primary);
                    }
                    setWorking(trigger, false, "What needs my attention?");
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (intelligenceResult != null) {
                        intelligenceResult.setText(
                                "I couldn’t complete the attention scan because an authorised source is unavailable.");
                        intelligenceResult.setTextColor(danger);
                    }
                    setWorking(trigger, false, "What needs my attention?");
                });
            }
        });
    }

    private void setWorking(Button button, boolean working, String label) {
        button.setEnabled(!working);
        button.setAlpha(working ? .6f : 1f);
        button.setText(label);
    }

    private static String promptFor(IntentRouter.Intent i) {
        if (i == IntentRouter.Intent.GUARDIAN) return "guardian status";
        if (i == IntentRouter.Intent.SUPPORT) return "support queue";
        if (i == IntentRouter.Intent.INVENTORY) return "inventory health";
        if (i == IntentRouter.Intent.CATALOGUE) return "catalogue health";
        if (i == IntentRouter.Intent.LEARNING) return "nova learning accuracy";
        if (i == IntentRouter.Intent.RELEASES) return "release state";
        return "business performance and profit";
    }

    private static String labelFor(IntentRouter.Intent i) {
        if (i == IntentRouter.Intent.GUARDIAN) return "Guardian";
        if (i == IntentRouter.Intent.SUPPORT) return "Support";
        if (i == IntentRouter.Intent.INVENTORY) return "Inventory";
        if (i == IntentRouter.Intent.CATALOGUE) return "Catalogue";
        if (i == IntentRouter.Intent.LEARNING) return "Learning";
        if (i == IntentRouter.Intent.RELEASES) return "Release & feature state";
        return "Business overview";
    }

    private void addChat(String who, String value, boolean user) {
        ChatLine line = new ChatLine(who, value, user);
        chatHistory.add(line);
        addBubbleView(line);
    }

    private void addBubbleView(ChatLine line) {
        if (conversation == null) return;

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(line.user ? Gravity.END : Gravity.START);

        if (!line.user) {
            TextView avatar = text("N", 15, primary);
            avatar.setGravity(Gravity.CENTER);
            avatar.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            avatar.setBackground(gradient(28, accent, Color.rgb(39, 93, 170), violet));
            LinearLayout.LayoutParams avatarLp = new LinearLayout.LayoutParams(dp(this, 46), dp(this, 46));
            avatarLp.setMarginEnd(dp(this, 8));
            row.addView(avatar, avatarLp);
        }

        LinearLayout bubble = new LinearLayout(this);
        bubble.setOrientation(LinearLayout.VERTICAL);
        bubble.setPadding(dp(this, 14), dp(this, 11), dp(this, 14), dp(this, 12));
        bubble.setBackground(line.user
                ? gradient(18, Color.rgb(72, 87, 255), Color.rgb(28, 62, 162), Color.rgb(57, 38, 170))
                : gradient(18, outlineBlue, Color.rgb(6, 28, 50), Color.rgb(8, 22, 40)));

        LinearLayout labelRow = new LinearLayout(this);
        labelRow.setOrientation(LinearLayout.HORIZONTAL);
        TextView label = text(line.who, 11, line.user ? accent : Color.rgb(95, 139, 255));
        label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        TextView time = text("  " + line.time, 10, Color.rgb(92, 119, 163));
        labelRow.addView(label);
        labelRow.addView(time);
        bubble.addView(labelRow);

        TextView body = text(line.value, 14, primary);
        body.setPadding(0, dp(this, 5), 0, 0);
        bubble.addView(body);

        LinearLayout.LayoutParams bubbleLp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, line.user ? .68f : .86f);

        if (line.user) {
            LinearLayout spacer = new LinearLayout(this);
            row.addView(spacer, new LinearLayout.LayoutParams(
                    0, 1, .32f));
            row.addView(bubble, bubbleLp);

            TextView avatar = text("◉", 15, accent);
            avatar.setGravity(Gravity.CENTER);
            avatar.setBackground(rounded(Color.rgb(8, 27, 50), 24, violet));
            LinearLayout.LayoutParams avatarLp = new LinearLayout.LayoutParams(dp(this, 42), dp(this, 42));
            avatarLp.setMarginStart(dp(this, 8));
            row.addView(avatar, avatarLp);
        } else {
            row.addView(bubble, bubbleLp);
        }

        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(this, 10);
        conversation.addView(row, p);

        if (scroll != null) scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
    }

    private void addOtaSection() {
        addOtaSection(true);
    }

    private void addOtaSection(boolean includeHeading) {
        if (includeHeading) section("Updates", "Signed Nova releases are verified before installation.");
        LinearLayout c = card();
        add(c, 8);
        updateStatus = text("Secure signed update channel ready.", 13, secondary);
        c.addView(updateStatus);
        updateButton = button(pendingUpdate == null
                ? "Check for updates"
                : "Install update " + pendingUpdate.versionName);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(this, 9);
        c.addView(updateButton, p);
        updateButton.setOnClickListener(v -> {
            updateButton.setEnabled(false);
            updateButton.setAlpha(.6f);
            if (pendingUpdate == null) updateManager.checkForUpdates();
            else updateManager.downloadAndInstall(pendingUpdate);
        });
    }

    private void footer() {
        LinearLayout f = new LinearLayout(this);
        f.setOrientation(LinearLayout.HORIZONTAL);
        f.setGravity(Gravity.CENTER_VERTICAL);
        f.setPadding(0, dp(this, 20), 0, dp(this, 8));

        TextView brand = text("© 2026 Morley Buys  •  Backed by Nova AI", 11, secondary);
        brand.setGravity(Gravity.CENTER);
        TextView secure = text("◈  Secure", 11, Color.rgb(102, 156, 255));
        secure.setGravity(Gravity.END);

        f.addView(brand, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        f.addView(secure);
        root.addView(f);
    }

    @Override
    public void onStatus(String m) {
        if (updateStatus != null) updateStatus.setText(m);
        if (updateButton != null) {
            updateButton.setEnabled(true);
            updateButton.setAlpha(1f);
        }
    }

    @Override
    public void onUpdateAvailable(UpdateManager.UpdateInfo i) {
        pendingUpdate = i;
        if (updateStatus != null) updateStatus.setText("Nova " + i.versionName + " is available.\n" + i.notes);
        if (updateButton != null) {
            updateButton.setText("Install update " + i.versionName);
            updateButton.setEnabled(true);
            updateButton.setAlpha(1f);
        }
    }

    @Override
    public void onUpToDate() {
        pendingUpdate = null;
        if (updateStatus != null) updateStatus.setText("Nova is up to date.");
        if (updateButton != null) {
            updateButton.setText("Check again");
            updateButton.setEnabled(true);
            updateButton.setAlpha(1f);
        }
    }

    @Override
    public void onError(String m) {
        if (updateStatus != null) updateStatus.setText("Update check failed. Please try again.");
        if (updateButton != null) {
            updateButton.setText("Retry update check");
            updateButton.setEnabled(true);
            updateButton.setAlpha(1f);
        }
    }

    @Override
    protected void onDestroy() {
        if (captchaChallenge != null) captchaChallenge.destroy();
        if (brainView != null) brainView.release();
        worker.shutdownNow();
        super.onDestroy();
    }

    private final class NovaBrainView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        private final RectF oval = new RectF();
        private ValueAnimator animator;
        private float phase;
        private boolean thinking;

        NovaBrainView(Context context) {
            super(context);
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            startAnimator();
        }

        void setThinking(boolean value) {
            thinking = value;
            invalidate();
        }

        void release() {
            if (animator != null) animator.cancel();
        }

        private void startAnimator() {
            animator = ValueAnimator.ofFloat(0f, 1f);
            animator.setDuration(1800);
            animator.setRepeatCount(ValueAnimator.INFINITE);
            animator.addUpdateListener(a -> {
                phase = (float) a.getAnimatedValue();
                invalidate();
            });
            animator.start();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float w = getWidth();
            float h = getHeight();
            float cx = w / 2f;
            float cy = h / 2f - dp(MainActivity.this, 2);
            float r = Math.min(w, h) * .34f;

            int pulse = thinking
                    ? blend(accent, pink, phase)
                    : blend(accent, violet, phase * .45f);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);

            paint.setColor(Color.argb(70, Color.red(violet), Color.green(violet), Color.blue(violet)));
            paint.setStrokeWidth(dp(MainActivity.this, 10));
            paint.setShadowLayer(dp(MainActivity.this, 14), 0, 0, pulse);
            canvas.drawCircle(cx, cy, r * 1.12f, paint);

            paint.clearShadowLayer();
            paint.setColor(pulse);
            paint.setStrokeWidth(dp(MainActivity.this, 2));
            canvas.drawCircle(cx, cy, r * 1.02f, paint);

            paint.setColor(Color.rgb(183, 207, 255));
            paint.setStrokeWidth(dp(MainActivity.this, 1));

            float left = cx - r * .72f;
            float top = cy - r * .62f;
            float right = cx + r * .72f;
            float bottom = cy + r * .62f;
            oval.set(left, top, right, bottom);
            canvas.drawOval(oval, paint);
            canvas.drawLine(cx, top + r * .05f, cx, bottom - r * .05f, paint);

            path.reset();
            path.moveTo(cx - r * .08f, top + r * .08f);
            path.cubicTo(cx - r * .52f, top - r * .04f, cx - r * .82f, cy - r * .18f, cx - r * .48f, cy);
            path.cubicTo(cx - r * .86f, cy + r * .16f, cx - r * .56f, bottom + r * .03f, cx - r * .12f, bottom - r * .10f);
            canvas.drawPath(path, paint);

            path.reset();
            path.moveTo(cx + r * .08f, top + r * .08f);
            path.cubicTo(cx + r * .52f, top - r * .04f, cx + r * .82f, cy - r * .18f, cx + r * .48f, cy);
            path.cubicTo(cx + r * .86f, cy + r * .16f, cx + r * .56f, bottom + r * .03f, cx + r * .12f, bottom - r * .10f);
            canvas.drawPath(path, paint);

            for (int i = -2; i <= 2; i++) {
                float y = cy + i * r * .20f;
                path.reset();
                path.moveTo(cx - r * .62f, y);
                path.cubicTo(cx - r * .35f, y - r * .13f, cx - r * .28f, y + r * .13f, cx - r * .04f, y);
                canvas.drawPath(path, paint);
                path.reset();
                path.moveTo(cx + r * .62f, y);
                path.cubicTo(cx + r * .35f, y - r * .13f, cx + r * .28f, y + r * .13f, cx + r * .04f, y);
                canvas.drawPath(path, paint);
            }

            paint.setColor(Color.argb(100, Color.red(accent), Color.green(accent), Color.blue(accent)));
            paint.setStrokeWidth(dp(MainActivity.this, 1));
            oval.set(cx - r * 1.42f, cy - r * .46f, cx + r * 1.42f, cy + r * .46f);
            canvas.drawOval(oval, paint);
            oval.set(cx - r * 1.18f, cy - r * .78f, cx + r * 1.18f, cy + r * .78f);
            canvas.save();
            canvas.rotate(48, cx, cy);
            canvas.drawOval(oval, paint);
            canvas.restore();
        }

        private int blend(int a, int b, float t) {
            float f = Math.max(0f, Math.min(1f, t));
            return Color.rgb(
                    Math.round(Color.red(a) + (Color.red(b) - Color.red(a)) * f),
                    Math.round(Color.green(a) + (Color.green(b) - Color.green(a)) * f),
                    Math.round(Color.blue(a) + (Color.blue(b) - Color.blue(a)) * f));
        }
    }
}
