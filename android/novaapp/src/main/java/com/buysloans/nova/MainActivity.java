package com.buysloans.nova;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
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
    private int background, surface, primary, secondary, accent, success, danger;

    private static int dp(Activity a,int v){return Math.round(v*a.getResources().getDisplayMetrics().density);}
    private TextView text(String value,float size,int color){TextView v=new TextView(this);v.setText(value);v.setTextSize(size);v.setTextColor(color);return v;}
    private Button button(String label){Button b=new Button(this);b.setText(label);b.setAllCaps(false);return b;}
    private void add(View v,int top){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT);p.topMargin=dp(this,top);root.addView(v,p);}

    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        background=Color.rgb(8,12,22);surface=Color.rgb(19,27,43);primary=Color.rgb(237,241,249);secondary=Color.rgb(166,178,199);accent=Color.rgb(124,156,255);success=Color.rgb(92,214,151);danger=Color.rgb(255,118,118);
        updateManager=new UpdateManager(this,this);
        showLogin();
        updateManager.checkForUpdates();
    }

    private void base(String subtitle){
        if(captchaChallenge!=null){captchaChallenge.destroy();captchaChallenge=null;}
        scroll=new ScrollView(this);scroll.setBackgroundColor(background);
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(this,22),dp(this,34),dp(this,22),dp(this,32));
        scroll.addView(root,new ScrollView.LayoutParams(ScrollView.LayoutParams.MATCH_PARENT,ScrollView.LayoutParams.WRAP_CONTENT));
        TextView brand=text("NOVA AI",14,accent);brand.setLetterSpacing(.18f);root.addView(brand);
        TextView title=text("Nova Control",32,primary);title.setPadding(0,dp(this,7),0,dp(this,3));root.addView(title);
        TextView sub=text(subtitle,15,secondary);sub.setPadding(0,0,0,dp(this,20));root.addView(sub);
        setContentView(scroll);
    }

    private EditText input(String hint,boolean password){
        EditText e=new EditText(this);e.setHint(hint);e.setHintTextColor(secondary);e.setTextColor(primary);e.setSingleLine(true);e.setPadding(dp(this,12),dp(this,11),dp(this,12),dp(this,11));
        e.setInputType(password?InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD:InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);return e;
    }

    private LinearLayout card(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(this,17),dp(this,17),dp(this,17),dp(this,17));c.setBackgroundColor(surface);return c;}

    private void showLogin(){
        conversation=null;base("Your standalone Morley intelligence companion");
        LinearLayout c=card();root.addView(c);
        c.addView(text("Sign in to Morley",21,primary));
        TextView note=text("Use your existing authorised Morley Admin account. Nova can read authorised Morley intelligence; protected writes remain unavailable.",14,secondary);note.setPadding(0,dp(this,8),0,dp(this,8));c.addView(note);
        EditText email=input("Email",false),password=input("Password",true);c.addView(email);c.addView(password);
        TextView challengeStatus=text("Security check loading…",13,secondary);challengeStatus.setPadding(0,dp(this,8),0,dp(this,4));c.addView(challengeStatus);
        Button signIn=button("Complete security check");signIn.setEnabled(false);
        captchaChallenge=new CaptchaChallenge(this,(ready,message)->{challengeStatus.setText(message);challengeStatus.setTextColor(ready?success:secondary);signIn.setEnabled(ready);signIn.setText(ready?"Sign in securely":"Complete security check");});
        c.addView(captchaChallenge.view());TextView status=text("",13,secondary);c.addView(status);c.addView(signIn);
        signIn.setOnClickListener(v->{String e=email.getText().toString().trim(),p=password.getText().toString();if(e.isEmpty()||p.isEmpty()){status.setText("Enter your email and password.");return;}if(captchaChallenge==null||!captchaChallenge.isReady()){status.setText("Complete the security check first.");return;}String token=captchaChallenge.token();signIn.setEnabled(false);status.setTextColor(secondary);status.setText("Verifying your authorised Morley account…");worker.execute(()->{try{api.signIn(e,p,token);runOnUiThread(this::showWorkspace);}catch(Exception ex){runOnUiThread(()->{status.setText(ex.getMessage());status.setTextColor(danger);if(captchaChallenge!=null)captchaChallenge.reset();});}});});
        addOtaSection();footer();
    }

    private void showWorkspace(){
        base("Live Morley intelligence • "+api.signedInEmail());
        LinearLayout statusCard=card();root.addView(statusCard);statusCard.addView(text("●  Nova intelligence online",20,success));
        TextView boundary=text("Live authorised data, Nova knowledge and verified learning are connected. Guardian approvals, pricing approvals, production releases and protected repairs stay behind human approval.",14,secondary);boundary.setPadding(0,dp(this,6),0,0);statusCard.addView(boundary);

        TextView quick=text("Quick intelligence",21,primary);quick.setPadding(0,dp(this,24),0,dp(this,8));root.addView(quick);
        Button attention=button("What needs my attention?");attention.setOnClickListener(v->runAttention());add(attention,0);
        addActionRow("Business overview",IntentRouter.Intent.PERFORMANCE,"Guardian",IntentRouter.Intent.GUARDIAN);
        addActionRow("Support",IntentRouter.Intent.SUPPORT,"Inventory",IntentRouter.Intent.INVENTORY);
        addActionRow("Catalogue",IntentRouter.Intent.CATALOGUE,"Learning",IntentRouter.Intent.LEARNING);
        Button releases=button("Release & feature state");releases.setOnClickListener(v->runSummary(IntentRouter.Intent.RELEASES));add(releases,8);

        TextView chatTitle=text("Ask Nova",23,primary);chatTitle.setPadding(0,dp(this,28),0,dp(this,5));root.addView(chatTitle);
        root.addView(text("Ask a natural follow-up, search stored Nova knowledge, or query live Morley operations. Conversation context stays in this app session only.",14,secondary));
        conversation=new LinearLayout(this);conversation.setOrientation(LinearLayout.VERTICAL);add(conversation,10);
        addBubble("Nova","Hi — I’m ready. Ask me what is happening in Morley, what needs attention, or about a stored process or decision.",false);
        EditText ask=input("Ask Nova…",false);ask.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES|InputType.TYPE_TEXT_FLAG_MULTI_LINE);ask.setSingleLine(false);add(ask,10);
        Button send=button("Ask Nova");add(send,7);send.setOnClickListener(v->{String q=ask.getText().toString().trim();if(q.isEmpty())return;ask.setText("");addBubble("You",q,true);send.setEnabled(false);worker.execute(()->{try{String reply=assistant.answer(q);runOnUiThread(()->{addBubble("Nova",reply,false);send.setEnabled(true);});}catch(Exception e){runOnUiThread(()->{addBubble("Nova","I understood the request, but an authorised source is unavailable: "+e.getMessage()+"\n\nNo protected change was made.",false);send.setEnabled(true);});}});});
        Button clear=button("Clear conversation");add(clear,7);clear.setOnClickListener(v->{conversation.removeAllViews();addBubble("Nova","Conversation cleared. Live Morley access remains connected.",false);});
        Button logout=button("Sign out of Nova");add(logout,22);logout.setOnClickListener(v->{api.signOut();showLogin();});
        addOtaSection();footer();
    }

    private void addActionRow(String left,IntentRouter.Intent li,String right,IntentRouter.Intent ri){
        LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);
        Button a=button(left),b=button(right);a.setOnClickListener(v->runSummary(li));b.setOnClickListener(v->runSummary(ri));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f);lp.setMarginEnd(dp(this,4));
        LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f);rp.setMarginStart(dp(this,4));row.addView(a,lp);row.addView(b,rp);add(row,8);
    }

    private void runSummary(IntentRouter.Intent intent){
        addBubble("Nova","Reading live Morley data…",false);
        worker.execute(()->{try{String result=assistant.answer(promptFor(intent));runOnUiThread(()->addBubble("Nova",result,false));}catch(Exception e){runOnUiThread(()->addBubble("Nova","The authorised live source is unavailable: "+e.getMessage(),false));}});
    }

    private void runAttention(){
        addBubble("Nova","Checking the highest-priority live signals…",false);
        worker.execute(()->{try{String result=assistant.attention();runOnUiThread(()->addBubble("Nova",result,false));}catch(Exception e){runOnUiThread(()->addBubble("Nova","I couldn’t complete the attention scan: "+e.getMessage(),false));}});
    }

    private static String promptFor(IntentRouter.Intent i){
        if(i==IntentRouter.Intent.GUARDIAN)return "guardian status";if(i==IntentRouter.Intent.SUPPORT)return "support queue";if(i==IntentRouter.Intent.INVENTORY)return "inventory health";if(i==IntentRouter.Intent.CATALOGUE)return "catalogue health";if(i==IntentRouter.Intent.LEARNING)return "nova learning accuracy";if(i==IntentRouter.Intent.RELEASES)return "release state";return "business performance and profit";
    }

    private void addBubble(String who,String value,boolean user){
        if(conversation==null)return;
        LinearLayout c=card();TextView label=text(who,12,user?accent:success);c.addView(label);TextView body=text(value,15,primary);body.setPadding(0,dp(this,4),0,0);c.addView(body);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT);p.topMargin=dp(this,7);conversation.addView(c,p);
        if(scroll!=null)scroll.post(()->scroll.smoothScrollTo(0,conversation.getBottom()));
    }

    private void addOtaSection(){TextView t=text("OTA updates",20,primary);t.setPadding(0,dp(this,28),0,dp(this,8));root.addView(t);updateStatus=text("Secure signed update channel ready.",14,secondary);root.addView(updateStatus);updateButton=button(pendingUpdate==null?"Check for updates":"Install update "+pendingUpdate.versionName);add(updateButton,9);updateButton.setOnClickListener(v->{if(pendingUpdate==null)updateManager.checkForUpdates();else updateManager.downloadAndInstall(pendingUpdate);});}
    private void footer(){TextView b=text("Backed by Nova AI",13,secondary);b.setGravity(Gravity.CENTER_HORIZONTAL);b.setPadding(0,dp(this,34),0,dp(this,8));root.addView(b);}

    @Override public void onStatus(String m){if(updateStatus!=null)updateStatus.setText(m);if(updateButton!=null)updateButton.setEnabled(true);}
    @Override public void onUpdateAvailable(UpdateManager.UpdateInfo i){pendingUpdate=i;if(updateStatus!=null)updateStatus.setText("Nova "+i.versionName+" is available.\n"+i.notes);if(updateButton!=null){updateButton.setText("Install update "+i.versionName);updateButton.setEnabled(true);}}
    @Override public void onUpToDate(){pendingUpdate=null;if(updateStatus!=null)updateStatus.setText("Nova is up to date.");if(updateButton!=null){updateButton.setText("Check again");updateButton.setEnabled(true);}}
    @Override public void onError(String m){if(updateStatus!=null)updateStatus.setText(m);if(updateButton!=null){updateButton.setText("Retry update check");updateButton.setEnabled(true);}}
    @Override protected void onDestroy(){if(captchaChallenge!=null)captchaChallenge.destroy();worker.shutdownNow();super.onDestroy();}
}
