package com.buysloans.nova;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity implements UpdateManager.Listener {
    private enum Tab { CHAT, INTELLIGENCE, UPDATES, ACCOUNT }
    private static final class ChatLine {
        final String who, value; final boolean user;
        ChatLine(String who,String value,boolean user){this.who=who;this.value=value;this.user=user;}
    }

    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final NovaApiClient api = new NovaApiClient();
    private final NovaAssistantEngine assistant = new NovaAssistantEngine(api);
    private final List<ChatLine> chatHistory = new ArrayList<>();
    private LinearLayout root, conversation, shell;
    private ScrollView scroll;
    private TextView updateStatus, intelligenceResult;
    private Button updateButton;
    private UpdateManager updateManager;
    private UpdateManager.UpdateInfo pendingUpdate;
    private CaptchaChallenge captchaChallenge;
    private Tab activeTab = Tab.CHAT;
    private int background, surface, surfaceRaised, primary, secondary, accent, accentStrong, success, danger, outline;

    private static int dp(Activity a,int v){return Math.round(v*a.getResources().getDisplayMetrics().density);}

    private TextView text(String value,float size,int color){
        TextView v=new TextView(this);v.setText(value);v.setTextSize(size);v.setTextColor(color);v.setLineSpacing(0,1.08f);return v;
    }

    private GradientDrawable rounded(int fill,int radius,int strokeColor){
        GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(this,radius));if(strokeColor!=Color.TRANSPARENT)d.setStroke(dp(this,1),strokeColor);return d;
    }

    private Button button(String label){return button(label,false);}
    private Button button(String label,boolean primaryAction){
        Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextSize(14);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setTextColor(primary);
        b.setMinHeight(dp(this,48));b.setPadding(dp(this,14),dp(this,10),dp(this,14),dp(this,10));
        b.setBackground(rounded(primaryAction?accentStrong:surfaceRaised,14,primaryAction?accent:outline));return b;
    }

    private Button navButton(String label,Tab tab){
        boolean selected=tab==activeTab;Button b=button(label,selected);b.setTextSize(12);b.setMinHeight(dp(this,52));b.setOnClickListener(v->{activeTab=tab;showWorkspace();});return b;
    }

    private void add(View v,int top){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT);p.topMargin=dp(this,top);root.addView(v,p);}

    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        background=Color.rgb(7,11,20);surface=Color.rgb(18,26,41);surfaceRaised=Color.rgb(28,38,57);primary=Color.rgb(239,243,250);secondary=Color.rgb(167,179,199);accent=Color.rgb(124,156,255);accentStrong=Color.rgb(74,105,214);success=Color.rgb(89,213,150);danger=Color.rgb(255,118,118);outline=Color.rgb(53,67,91);
        updateManager=new UpdateManager(this,this);showLogin();updateManager.checkForUpdates();
    }

    private void base(String subtitle,boolean withNav){
        if(captchaChallenge!=null){captchaChallenge.destroy();captchaChallenge=null;}
        shell=new LinearLayout(this);shell.setOrientation(LinearLayout.VERTICAL);shell.setBackgroundColor(background);
        scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(background);
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(this,20),dp(this,28),dp(this,20),dp(this,30));
        scroll.addView(root,new ScrollView.LayoutParams(ScrollView.LayoutParams.MATCH_PARENT,ScrollView.LayoutParams.WRAP_CONTENT));
        shell.addView(scroll,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,0,1f));

        LinearLayout brandRow=new LinearLayout(this);brandRow.setOrientation(LinearLayout.HORIZONTAL);brandRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand=text("NOVA AI",13,accent);brand.setLetterSpacing(.18f);brand.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        TextView version=text("v"+BuildConfig.VERSION_NAME,12,secondary);version.setGravity(Gravity.END);
        brandRow.addView(brand,new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f));brandRow.addView(version);root.addView(brandRow);
        TextView title=text("Nova Control",30,primary);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);title.setPadding(0,dp(this,8),0,dp(this,4));root.addView(title);
        TextView sub=text(subtitle,14,secondary);sub.setPadding(0,0,0,dp(this,18));root.addView(sub);

        if(withNav){
            LinearLayout nav=new LinearLayout(this);nav.setOrientation(LinearLayout.HORIZONTAL);nav.setPadding(dp(this,8),dp(this,7),dp(this,8),dp(this,7));nav.setBackgroundColor(surface);
            nav.addView(navButton("Chat",Tab.CHAT),new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f));
            nav.addView(navButton("Intelligence",Tab.INTELLIGENCE),new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f));
            nav.addView(navButton("Updates",Tab.UPDATES),new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f));
            nav.addView(navButton("Account",Tab.ACCOUNT),new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f));
            shell.addView(nav,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT));
        }
        setContentView(shell);
    }

    private EditText input(String hint,boolean password){
        EditText e=new EditText(this);e.setHint(hint);e.setHintTextColor(secondary);e.setTextColor(primary);e.setTextSize(15);e.setSingleLine(true);e.setPadding(dp(this,14),dp(this,12),dp(this,14),dp(this,12));e.setBackground(rounded(surfaceRaised,14,outline));
        e.setInputType(password?InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD:InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);return e;
    }

    private LinearLayout card(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(this,16),dp(this,16),dp(this,16),dp(this,16));c.setBackground(rounded(surface,18,outline));return c;}

    private void section(String title,String subtitle){
        LinearLayout wrap=new LinearLayout(this);wrap.setOrientation(LinearLayout.VERTICAL);TextView h=text(title,20,primary);h.setTypeface(Typeface.DEFAULT,Typeface.BOLD);wrap.addView(h);
        if(subtitle!=null&&!subtitle.isBlank()){TextView s=text(subtitle,13,secondary);s.setPadding(0,dp(this,3),0,0);wrap.addView(s);}add(wrap,24);
    }

    private void watch(EditText input,Runnable changed){input.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int count,int after){}public void onTextChanged(CharSequence s,int st,int before,int count){changed.run();}public void afterTextChanged(Editable e){}});}

    private void showLogin(){
        assistant.resetContext();chatHistory.clear();conversation=null;base("Your standalone Morley intelligence companion",false);
        LinearLayout c=card();add(c,2);TextView heading=text("Sign in to Morley",21,primary);heading.setTypeface(Typeface.DEFAULT,Typeface.BOLD);c.addView(heading);
        TextView note=text("Use your authorised Morley Admin account. Nova can read approved intelligence; protected writes remain unavailable.",14,secondary);note.setPadding(0,dp(this,7),0,dp(this,12));c.addView(note);
        EditText email=input("Email",false),password=input("Password",true);c.addView(email);LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT);pp.topMargin=dp(this,8);c.addView(password,pp);
        TextView challengeStatus=text("Security check loading…",13,secondary);challengeStatus.setPadding(0,dp(this,10),0,dp(this,5));c.addView(challengeStatus);Button signIn=button("Complete security check",true);signIn.setEnabled(false);signIn.setAlpha(.55f);boolean[] captchaReady={false};
        Runnable sync=()->{boolean ready=captchaReady[0]&&email.getText().toString().trim().contains("@")&&!password.getText().toString().isBlank();signIn.setEnabled(ready);signIn.setAlpha(ready?1f:.55f);signIn.setText(captchaReady[0]?"Sign in securely":"Complete security check");};
        captchaChallenge=new CaptchaChallenge(this,(ready,message)->{captchaReady[0]=ready;challengeStatus.setText(message);challengeStatus.setTextColor(ready?success:secondary);sync.run();});c.addView(captchaChallenge.view());
        TextView status=text("",13,secondary);status.setPadding(0,dp(this,6),0,dp(this,4));c.addView(status);c.addView(signIn);watch(email,sync);watch(password,sync);
        signIn.setOnClickListener(v->{String e=email.getText().toString().trim(),p=password.getText().toString();if(e.isEmpty()||p.isEmpty()){status.setText("Enter your email and password.");status.setTextColor(danger);return;}if(captchaChallenge==null||!captchaChallenge.isReady()){status.setText("Complete the security check first.");status.setTextColor(danger);return;}String token=captchaChallenge.token();signIn.setEnabled(false);signIn.setAlpha(.55f);status.setTextColor(secondary);status.setText("Verifying your authorised Morley account…");worker.execute(()->{try{api.signIn(e,p,token);runOnUiThread(()->{activeTab=Tab.CHAT;showWorkspace();});}catch(Exception ex){runOnUiThread(()->{status.setText("Sign-in failed. Check your details and try again.");status.setTextColor(danger);captchaReady[0]=false;if(captchaChallenge!=null)captchaChallenge.reset();sync.run();});}});});
        addOtaSection();footer();
    }

    private void showWorkspace(){
        base(activeTab==Tab.CHAT?"Conversation":activeTab==Tab.INTELLIGENCE?"Live Morley intelligence":activeTab==Tab.UPDATES?"Secure signed releases":"Account & authority",true);
        if(activeTab==Tab.CHAT)showChatTab();else if(activeTab==Tab.INTELLIGENCE)showIntelligenceTab();else if(activeTab==Tab.UPDATES)showUpdatesTab();else showAccountTab();
    }

    private void showChatTab(){
        section("Ask Nova","Talk normally or ask about Morley. Nova keeps the current topic during this session.");
        LinearLayout chatCard=card();add(chatCard,8);conversation=new LinearLayout(this);conversation.setOrientation(LinearLayout.VERTICAL);chatCard.addView(conversation);
        if(chatHistory.isEmpty())chatHistory.add(new ChatLine("Nova","Hi — I’m ready. We can chat normally, or you can ask what needs attention, what is happening in Morley, or about a stored process or decision.",false));
        for(ChatLine line:chatHistory)addBubbleView(line.who,line.value,line.user);
        EditText ask=input("Message Nova…",false);ask.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES|InputType.TYPE_TEXT_FLAG_MULTI_LINE);ask.setSingleLine(false);ask.setMinLines(2);
        LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT);ap.topMargin=dp(this,10);chatCard.addView(ask,ap);
        Button send=button("Send",true);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT);sp.topMargin=dp(this,8);chatCard.addView(send,sp);
        Button clear=button("Clear conversation");LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT);cp.topMargin=dp(this,7);chatCard.addView(clear,cp);
        send.setOnClickListener(v->sendQuestion(ask,send));clear.setOnClickListener(v->{assistant.resetContext();chatHistory.clear();conversation.removeAllViews();addChat("Nova","Conversation cleared. We can start fresh — what’s on your mind?",false);});footer();
    }

    private void showIntelligenceTab(){
        LinearLayout statusCard=card();add(statusCard,0);TextView online=text("●  Nova intelligence online",18,success);online.setTypeface(Typeface.DEFAULT,Typeface.BOLD);statusCard.addView(online);TextView boundary=text("Live authorised data, Nova knowledge and verified learning are connected. Protected approvals and repairs remain human-gated.",13,secondary);boundary.setPadding(0,dp(this,8),0,0);statusCard.addView(boundary);
        section("Quick intelligence","Each section now lives here instead of being mixed into the chat screen.");
        LinearLayout result=card();add(result,8);intelligenceResult=text("Choose a live intelligence view below.",14,secondary);result.addView(intelligenceResult);
        Button attention=button("What needs my attention?",true);attention.setOnClickListener(v->runAttention(attention));add(attention,10);
        addActionRow("Business overview",IntentRouter.Intent.PERFORMANCE,"Guardian",IntentRouter.Intent.GUARDIAN);
        addActionRow("Support",IntentRouter.Intent.SUPPORT,"Inventory",IntentRouter.Intent.INVENTORY);
        addActionRow("Catalogue",IntentRouter.Intent.CATALOGUE,"Learning",IntentRouter.Intent.LEARNING);
        Button releases=button("Release & feature state");releases.setOnClickListener(v->runSummary(IntentRouter.Intent.RELEASES,releases));add(releases,8);footer();
    }

    private void showUpdatesTab(){addOtaSection();footer();}

    private void showAccountTab(){
        LinearLayout account=card();add(account,0);TextView online=text("●  Signed in",18,success);online.setTypeface(Typeface.DEFAULT,Typeface.BOLD);account.addView(online);
        TextView email=text(api.signedInEmail(),14,primary);email.setSingleLine(true);email.setEllipsize(TextUtils.TruncateAt.END);email.setPadding(0,dp(this,7),0,0);account.addView(email);
        TextView boundary=text("Nova can read authorised intelligence and knowledge. Protected approvals, pricing decisions, production releases and repairs remain human-gated.",13,secondary);boundary.setPadding(0,dp(this,10),0,0);account.addView(boundary);
        Button logout=button("Sign out of Nova");logout.setOnClickListener(v->{assistant.resetContext();chatHistory.clear();api.signOut();showLogin();});add(logout,18);footer();
    }

    private void sendQuestion(EditText ask,Button send){
        String q=ask.getText().toString().trim();if(q.isEmpty())return;ask.setText("");addChat("You",q,true);setWorking(send,true,"Nova is thinking…");
        worker.execute(()->{try{String reply=assistant.answer(q);runOnUiThread(()->{addChat("Nova",reply,false);setWorking(send,false,"Send");});}catch(Exception e){runOnUiThread(()->{addChat("Nova","I understood the request, but the authorised source is unavailable right now. We can keep talking, and no protected change was made.",false);setWorking(send,false,"Send");});}});
    }

    private void addActionRow(String left,IntentRouter.Intent li,String right,IntentRouter.Intent ri){LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);Button a=button(left),b=button(right);a.setOnClickListener(v->runSummary(li,a));b.setOnClickListener(v->runSummary(ri,b));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f);lp.setMarginEnd(dp(this,4));LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f);rp.setMarginStart(dp(this,4));row.addView(a,lp);row.addView(b,rp);add(row,8);}

    private void runSummary(IntentRouter.Intent intent,Button trigger){if(intelligenceResult!=null){intelligenceResult.setText("Reading live Morley data…");intelligenceResult.setTextColor(secondary);}setWorking(trigger,true,"Loading…");worker.execute(()->{try{String result=assistant.answer(promptFor(intent));runOnUiThread(()->{if(intelligenceResult!=null){intelligenceResult.setText(result);intelligenceResult.setTextColor(primary);}setWorking(trigger,false,labelFor(intent));});}catch(Exception e){runOnUiThread(()->{if(intelligenceResult!=null){intelligenceResult.setText("That authorised live source is unavailable right now.");intelligenceResult.setTextColor(danger);}setWorking(trigger,false,labelFor(intent));});}});}

    private void runAttention(Button trigger){if(intelligenceResult!=null){intelligenceResult.setText("Checking the highest-priority live signals…");intelligenceResult.setTextColor(secondary);}setWorking(trigger,true,"Checking attention…");worker.execute(()->{try{String result=assistant.attention();runOnUiThread(()->{if(intelligenceResult!=null){intelligenceResult.setText(result);intelligenceResult.setTextColor(primary);}setWorking(trigger,false,"What needs my attention?");});}catch(Exception e){runOnUiThread(()->{if(intelligenceResult!=null){intelligenceResult.setText("I couldn’t complete the attention scan because an authorised source is unavailable.");intelligenceResult.setTextColor(danger);}setWorking(trigger,false,"What needs my attention?");});}});}

    private void setWorking(Button button,boolean working,String label){button.setEnabled(!working);button.setAlpha(working?.6f:1f);button.setText(label);}

    private static String promptFor(IntentRouter.Intent i){if(i==IntentRouter.Intent.GUARDIAN)return "guardian status";if(i==IntentRouter.Intent.SUPPORT)return "support queue";if(i==IntentRouter.Intent.INVENTORY)return "inventory health";if(i==IntentRouter.Intent.CATALOGUE)return "catalogue health";if(i==IntentRouter.Intent.LEARNING)return "nova learning accuracy";if(i==IntentRouter.Intent.RELEASES)return "release state";return "business performance and profit";}
    private static String labelFor(IntentRouter.Intent i){if(i==IntentRouter.Intent.GUARDIAN)return "Guardian";if(i==IntentRouter.Intent.SUPPORT)return "Support";if(i==IntentRouter.Intent.INVENTORY)return "Inventory";if(i==IntentRouter.Intent.CATALOGUE)return "Catalogue";if(i==IntentRouter.Intent.LEARNING)return "Learning";if(i==IntentRouter.Intent.RELEASES)return "Release & feature state";return "Business overview";}

    private void addChat(String who,String value,boolean user){chatHistory.add(new ChatLine(who,value,user));addBubbleView(who,value,user);}
    private void addBubbleView(String who,String value,boolean user){if(conversation==null)return;LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(this,13),dp(this,11),dp(this,13),dp(this,11));c.setBackground(rounded(user?Color.rgb(31,43,75):surfaceRaised,14,user?accent:outline));TextView label=text(who,11,user?accent:success);label.setTypeface(Typeface.DEFAULT,Typeface.BOLD);c.addView(label);TextView body=text(value,14,primary);body.setPadding(0,dp(this,4),0,0);c.addView(body);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT);p.topMargin=dp(this,7);conversation.addView(c,p);if(scroll!=null)scroll.post(()->scroll.fullScroll(View.FOCUS_DOWN));}

    private void addOtaSection(){section("Updates","Signed Nova releases are verified before installation.");LinearLayout c=card();add(c,8);updateStatus=text("Secure signed update channel ready.",13,secondary);c.addView(updateStatus);updateButton=button(pendingUpdate==null?"Check for updates":"Install update "+pendingUpdate.versionName);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT);p.topMargin=dp(this,9);c.addView(updateButton,p);updateButton.setOnClickListener(v->{updateButton.setEnabled(false);updateButton.setAlpha(.6f);if(pendingUpdate==null)updateManager.checkForUpdates();else updateManager.downloadAndInstall(pendingUpdate);});}

    private void footer(){TextView b=text("© 2026 Morley Buys  •  Backed by Nova AI",12,secondary);b.setGravity(Gravity.CENTER_HORIZONTAL);b.setPadding(0,dp(this,28),0,dp(this,8));root.addView(b);}

    @Override public void onStatus(String m){if(updateStatus!=null)updateStatus.setText(m);if(updateButton!=null){updateButton.setEnabled(true);updateButton.setAlpha(1f);}}
    @Override public void onUpdateAvailable(UpdateManager.UpdateInfo i){pendingUpdate=i;if(updateStatus!=null)updateStatus.setText("Nova "+i.versionName+" is available.\n"+i.notes);if(updateButton!=null){updateButton.setText("Install update "+i.versionName);updateButton.setEnabled(true);updateButton.setAlpha(1f);}}
    @Override public void onUpToDate(){pendingUpdate=null;if(updateStatus!=null)updateStatus.setText("Nova is up to date.");if(updateButton!=null){updateButton.setText("Check again");updateButton.setEnabled(true);updateButton.setAlpha(1f);}}
    @Override public void onError(String m){if(updateStatus!=null)updateStatus.setText("Update check failed. Please try again.");if(updateButton!=null){updateButton.setText("Retry update check");updateButton.setEnabled(true);updateButton.setAlpha(1f);}}
    @Override protected void onDestroy(){if(captchaChallenge!=null)captchaChallenge.destroy();worker.shutdownNow();super.onDestroy();}
}
