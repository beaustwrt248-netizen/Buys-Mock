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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity implements UpdateManager.Listener {
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final NovaApiClient api = new NovaApiClient();
    private final NovaAssistantEngine assistant = new NovaAssistantEngine(api);
    private LinearLayout root;
    private LinearLayout conversation;
    private ScrollView scroll;
    private TextView updateStatus;
    private Button updateButton;
    private UpdateManager updateManager;
    private UpdateManager.UpdateInfo pendingUpdate;
    private CaptchaChallenge captchaChallenge;
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
        Button b=new Button(this);
        b.setText(label);b.setAllCaps(false);b.setTextSize(14);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setTextColor(primary);
        b.setMinHeight(dp(this,48));b.setPadding(dp(this,14),dp(this,10),dp(this,14),dp(this,10));
        b.setBackground(rounded(primaryAction?accentStrong:surfaceRaised,14,primaryAction?accent:outline));
        return b;
    }

    private void add(View v,int top){
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT);p.topMargin=dp(this,top);root.addView(v,p);
    }

    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        background=Color.rgb(7,11,20);surface=Color.rgb(18,26,41);surfaceRaised=Color.rgb(28,38,57);
        primary=Color.rgb(239,243,250);secondary=Color.rgb(167,179,199);accent=Color.rgb(124,156,255);accentStrong=Color.rgb(74,105,214);
        success=Color.rgb(89,213,150);danger=Color.rgb(255,118,118);outline=Color.rgb(53,67,91);
        updateManager=new UpdateManager(this,this);
        showLogin();
        updateManager.checkForUpdates();
    }

    private void base(String subtitle){
        if(captchaChallenge!=null){captchaChallenge.destroy();captchaChallenge=null;}
        scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(background);
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(this,20),dp(this,28),dp(this,20),dp(this,30));
        scroll.addView(root,new ScrollView.LayoutParams(ScrollView.LayoutParams.MATCH_PARENT,ScrollView.LayoutParams.WRAP_CONTENT));

        LinearLayout brandRow=new LinearLayout(this);brandRow.setOrientation(LinearLayout.HORIZONTAL);brandRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand=text("NOVA AI",13,accent);brand.setLetterSpacing(.18f);brand.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        TextView version=text("v"+BuildConfig.VERSION_NAME,12,secondary);version.setGravity(Gravity.END);
        brandRow.addView(brand,new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f));brandRow.addView(version);
        root.addView(brandRow);

        TextView title=text("Nova Control",30,primary);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);title.setPadding(0,dp(this,8),0,dp(this,4));root.addView(title);
        TextView sub=text(subtitle,14,secondary);sub.setPadding(0,0,0,dp(this,18));root.addView(sub);
        setContentView(scroll);
    }

    private EditText input(String hint,boolean password){
        EditText e=new EditText(this);e.setHint(hint);e.setHintTextColor(secondary);e.setTextColor(primary);e.setTextSize(15);e.setSingleLine(true);
        e.setPadding(dp(this,14),dp(this,12),dp(this,14),dp(this,12));e.setBackground(rounded(surfaceRaised,14,outline));
        e.setInputType(password?InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD:InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);return e;
    }

    private LinearLayout card(){
        LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(this,16),dp(this,16),dp(this,16),dp(this,16));c.setBackground(rounded(surface,18,outline));return c;
    }

    private TextView section(String title,String subtitle){
        LinearLayout wrap=new LinearLayout(this);wrap.setOrientation(LinearLayout.VERTICAL);
        TextView h=text(title,20,primary);h.setTypeface(Typeface.DEFAULT,Typeface.BOLD);wrap.addView(h);
        if(subtitle!=null&&!subtitle.isBlank()){TextView s=text(subtitle,13,secondary);s.setPadding(0,dp(this,3),0,0);wrap.addView(s);}
        add(wrap,24);return h;
    }

    private void watch(EditText input,Runnable changed){
        input.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int count,int after){}public void onTextChanged(CharSequence s,int st,int before,int count){changed.run();}public void afterTextChanged(Editable e){}});
    }

    private void showLogin(){
        assistant.resetContext();conversation=null;base("Your standalone Morley intelligence companion");
        LinearLayout c=card();add(c,2);
        TextView heading=text("Sign in to Morley",21,primary);heading.setTypeface(Typeface.DEFAULT,Typeface.BOLD);c.addView(heading);
        TextView note=text("Use your authorised Morley Admin account. Nova can read approved intelligence; protected writes remain unavailable.",14,secondary);note.setPadding(0,dp(this,7),0,dp(this,12));c.addView(note);
        EditText email=input("Email",false),password=input("Password",true);c.addView(email);LinearLayout.LayoutParams passwordParams=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT);passwordParams.topMargin=dp(this,8);c.addView(password,passwordParams);
        TextView challengeStatus=text("Security check loading…",13,secondary);challengeStatus.setPadding(0,dp(this,10),0,dp(this,5));c.addView(challengeStatus);
        Button signIn=button("Complete security check",true);signIn.setEnabled(false);signIn.setAlpha(.55f);
        boolean[] captchaReady={false};
        Runnable syncSignIn=()->{boolean ready=captchaReady[0]&&email.getText().toString().trim().contains("@")&&!password.getText().toString().isBlank();signIn.setEnabled(ready);signIn.setAlpha(ready?1f:.55f);signIn.setText(captchaReady[0]?"Sign in securely":"Complete security check");};
        captchaChallenge=new CaptchaChallenge(this,(ready,message)->{captchaReady[0]=ready;challengeStatus.setText(message);challengeStatus.setTextColor(ready?success:secondary);syncSignIn.run();});
        c.addView(captchaChallenge.view());TextView status=text("",13,secondary);status.setPadding(0,dp(this,6),0,dp(this,4));c.addView(status);c.addView(signIn);
        watch(email,syncSignIn);watch(password,syncSignIn);
        signIn.setOnClickListener(v->{
            String e=email.getText().toString().trim(),p=password.getText().toString();
            if(e.isEmpty()||p.isEmpty()){status.setText("Enter your email and password.");status.setTextColor(danger);return;}
            if(captchaChallenge==null||!captchaChallenge.isReady()){status.setText("Complete the security check first.");status.setTextColor(danger);return;}
            String token=captchaChallenge.token();signIn.setEnabled(false);signIn.setAlpha(.55f);status.setTextColor(secondary);status.setText("Verifying your authorised Morley account…");
            worker.execute(()->{try{api.signIn(e,p,token);runOnUiThread(this::showWorkspace);}catch(Exception ex){runOnUiThread(()->{status.setText("Sign-in failed. Check your details and try again.");status.setTextColor(danger);captchaReady[0]=false;if(captchaChallenge!=null)captchaChallenge.reset();syncSignIn.run();});}});
        });
        addOtaSection();footer();
    }

    private void showWorkspace(){
        base("Live Morley intelligence");

        LinearLayout account=card();add(account,0);
        TextView online=text("●  Nova intelligence online",18,success);online.setTypeface(Typeface.DEFAULT,Typeface.BOLD);account.addView(online);
        TextView email=text("Signed in • "+api.signedInEmail(),13,secondary);email.setSingleLine(true);email.setEllipsize(TextUtils.TruncateAt.END);email.setPadding(0,dp(this,6),0,0);account.addView(email);
        TextView boundary=text("Live authorised data, Nova knowledge and verified learning are connected. Protected approvals and repairs remain human-gated.",13,secondary);boundary.setPadding(0,dp(this,8),0,0);account.addView(boundary);

        section("Ask Nova","Ask naturally. Short follow-ups keep the current topic for this session.");
        LinearLayout chatCard=card();add(chatCard,8);
        conversation=new LinearLayout(this);conversation.setOrientation(LinearLayout.VERTICAL);chatCard.addView(conversation);
        addBubble("Nova","Hi — I’m ready. Ask what needs attention, what is happening in Morley, or about a stored process or decision.",false);
        EditText ask=input("Ask Nova…",false);ask.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES|InputType.TYPE_TEXT_FLAG_MULTI_LINE);ask.setSingleLine(false);ask.setMinLines(2);
        LinearLayout.LayoutParams askParams=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT);askParams.topMargin=dp(this,10);chatCard.addView(ask,askParams);
        Button send=button("Ask Nova",true);LinearLayout.LayoutParams sendParams=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT);sendParams.topMargin=dp(this,8);chatCard.addView(send,sendParams);
        Button clear=button("Clear conversation");LinearLayout.LayoutParams clearParams=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT);clearParams.topMargin=dp(this,7);chatCard.addView(clear,clearParams);
        send.setOnClickListener(v->sendQuestion(ask,send));
        clear.setOnClickListener(v->{assistant.resetContext();conversation.removeAllViews();addBubble("Nova","Conversation cleared. Ask me anything about the authorised Morley sources.",false);});

        section("Quick intelligence","One tap reads the latest authorised source and returns the answer above.");
        Button attention=button("What needs my attention?",true);attention.setOnClickListener(v->runAttention(attention));add(attention,8);
        addActionRow("Business overview",IntentRouter.Intent.PERFORMANCE,"Guardian",IntentRouter.Intent.GUARDIAN);
        addActionRow("Support",IntentRouter.Intent.SUPPORT,"Inventory",IntentRouter.Intent.INVENTORY);
        addActionRow("Catalogue",IntentRouter.Intent.CATALOGUE,"Learning",IntentRouter.Intent.LEARNING);
        Button releases=button("Release & feature state");releases.setOnClickListener(v->runSummary(IntentRouter.Intent.RELEASES,releases));add(releases,8);

        addOtaSection();
        Button logout=button("Sign out of Nova");logout.setOnClickListener(v->{assistant.resetContext();api.signOut();showLogin();});add(logout,18);
        footer();
    }

    private void sendQuestion(EditText ask,Button send){
        String q=ask.getText().toString().trim();if(q.isEmpty())return;
        ask.setText("");addBubble("You",q,true);setWorking(send,true,"Nova is thinking…");
        worker.execute(()->{try{String reply=assistant.answer(q);runOnUiThread(()->{addBubble("Nova",reply,false);setWorking(send,false,"Ask Nova");});}catch(Exception e){runOnUiThread(()->{addBubble("Nova","I understood the request, but the authorised source is unavailable right now. No protected change was made.",false);setWorking(send,false,"Ask Nova");});}});
    }

    private void addActionRow(String left,IntentRouter.Intent li,String right,IntentRouter.Intent ri){
        LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);
        Button a=button(left),b=button(right);a.setOnClickListener(v->runSummary(li,a));b.setOnClickListener(v->runSummary(ri,b));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f);lp.setMarginEnd(dp(this,4));
        LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f);rp.setMarginStart(dp(this,4));row.addView(a,lp);row.addView(b,rp);add(row,8);
    }

    private void runSummary(IntentRouter.Intent intent,Button trigger){
        addBubble("Nova","Reading live Morley data…",false);setWorking(trigger,true,"Loading…");
        worker.execute(()->{try{String result=assistant.answer(promptFor(intent));runOnUiThread(()->{addBubble("Nova",result,false);setWorking(trigger,false,labelFor(intent));});}catch(Exception e){runOnUiThread(()->{addBubble("Nova","That authorised live source is unavailable right now.",false);setWorking(trigger,false,labelFor(intent));});}});
    }

    private void runAttention(Button trigger){
        addBubble("Nova","Checking the highest-priority live signals…",false);setWorking(trigger,true,"Checking attention…");
        worker.execute(()->{try{String result=assistant.attention();runOnUiThread(()->{addBubble("Nova",result,false);setWorking(trigger,false,"What needs my attention?");});}catch(Exception e){runOnUiThread(()->{addBubble("Nova","I couldn’t complete the attention scan because an authorised source is unavailable.",false);setWorking(trigger,false,"What needs my attention?");});}});
    }

    private void setWorking(Button button,boolean working,String label){button.setEnabled(!working);button.setAlpha(working?.6f:1f);button.setText(label);}

    private static String promptFor(IntentRouter.Intent i){
        if(i==IntentRouter.Intent.GUARDIAN)return "guardian status";if(i==IntentRouter.Intent.SUPPORT)return "support queue";if(i==IntentRouter.Intent.INVENTORY)return "inventory health";if(i==IntentRouter.Intent.CATALOGUE)return "catalogue health";if(i==IntentRouter.Intent.LEARNING)return "nova learning accuracy";if(i==IntentRouter.Intent.RELEASES)return "release state";return "business performance and profit";
    }

    private static String labelFor(IntentRouter.Intent i){
        if(i==IntentRouter.Intent.GUARDIAN)return "Guardian";if(i==IntentRouter.Intent.SUPPORT)return "Support";if(i==IntentRouter.Intent.INVENTORY)return "Inventory";if(i==IntentRouter.Intent.CATALOGUE)return "Catalogue";if(i==IntentRouter.Intent.LEARNING)return "Learning";if(i==IntentRouter.Intent.RELEASES)return "Release & feature state";return "Business overview";
    }

    private void addBubble(String who,String value,boolean user){
        if(conversation==null)return;
        LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(this,13),dp(this,11),dp(this,13),dp(this,11));c.setBackground(rounded(user?Color.rgb(31,43,75):surfaceRaised,14,user?accent:outline));
        TextView label=text(who,11,user?accent:success);label.setTypeface(Typeface.DEFAULT,Typeface.BOLD);c.addView(label);TextView body=text(value,14,primary);body.setPadding(0,dp(this,4),0,0);c.addView(body);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT);p.topMargin=dp(this,7);conversation.addView(c,p);
        if(scroll!=null)scroll.post(()->{int y=Math.max(0,conversation.getTop()+conversation.getBottom()-dp(this,40));scroll.smoothScrollTo(0,y);});
    }

    private void addOtaSection(){
        section("Updates","Signed Nova releases are verified before installation.");
        LinearLayout c=card();add(c,8);updateStatus=text("Secure signed update channel ready.",13,secondary);c.addView(updateStatus);
        updateButton=button(pendingUpdate==null?"Check for updates":"Install update "+pendingUpdate.versionName);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT);p.topMargin=dp(this,9);c.addView(updateButton,p);
        updateButton.setOnClickListener(v->{updateButton.setEnabled(false);updateButton.setAlpha(.6f);if(pendingUpdate==null)updateManager.checkForUpdates();else updateManager.downloadAndInstall(pendingUpdate);});
    }

    private void footer(){TextView b=text("© 2026 Morley Buys  •  Backed by Nova AI",12,secondary);b.setGravity(Gravity.CENTER_HORIZONTAL);b.setPadding(0,dp(this,28),0,dp(this,8));root.addView(b);}

    @Override public void onStatus(String m){if(updateStatus!=null)updateStatus.setText(m);if(updateButton!=null){updateButton.setEnabled(true);updateButton.setAlpha(1f);}}
    @Override public void onUpdateAvailable(UpdateManager.UpdateInfo i){pendingUpdate=i;if(updateStatus!=null)updateStatus.setText("Nova "+i.versionName+" is available.\n"+i.notes);if(updateButton!=null){updateButton.setText("Install update "+i.versionName);updateButton.setEnabled(true);updateButton.setAlpha(1f);}}
    @Override public void onUpToDate(){pendingUpdate=null;if(updateStatus!=null)updateStatus.setText("Nova is up to date.");if(updateButton!=null){updateButton.setText("Check again");updateButton.setEnabled(true);updateButton.setAlpha(1f);}}
    @Override public void onError(String m){if(updateStatus!=null)updateStatus.setText("Update check failed. Please try again.");if(updateButton!=null){updateButton.setText("Retry update check");updateButton.setEnabled(true);updateButton.setAlpha(1f);}}
    @Override protected void onDestroy(){if(captchaChallenge!=null)captchaChallenge.destroy();worker.shutdownNow();super.onDestroy();}
}
