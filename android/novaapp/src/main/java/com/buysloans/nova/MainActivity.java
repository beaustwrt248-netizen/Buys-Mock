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

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity implements UpdateManager.Listener {
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final NovaApiClient api = new NovaApiClient();
    private LinearLayout root;
    private TextView updateStatus;
    private Button updateButton;
    private UpdateManager updateManager;
    private UpdateManager.UpdateInfo pendingUpdate;
    private CaptchaChallenge captchaChallenge;
    private int background, surface, primary, secondary, accent, success, danger;

    private static int dp(Activity activity, int value) { return Math.round(value * activity.getResources().getDisplayMetrics().density); }
    private TextView text(String value, float sizeSp, int color) { TextView v=new TextView(this); v.setText(value); v.setTextSize(sizeSp); v.setTextColor(color); return v; }
    private Button button(String label) { Button b=new Button(this); b.setText(label); b.setAllCaps(false); return b; }
    private void margin(View v, int top) { LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT); p.topMargin=dp(this,top); root.addView(v,p); }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        background=Color.rgb(8,12,22); surface=Color.rgb(19,27,43); primary=Color.rgb(237,241,249); secondary=Color.rgb(166,178,199); accent=Color.rgb(124,156,255); success=Color.rgb(92,214,151); danger=Color.rgb(255,118,118);
        updateManager=new UpdateManager(this,this);
        showLogin();
        updateManager.checkForUpdates();
    }

    private void base(String subtitle) {
        if(captchaChallenge!=null){captchaChallenge.destroy();captchaChallenge=null;}
        ScrollView scroll=new ScrollView(this); scroll.setBackgroundColor(background);
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(this,24),dp(this,38),dp(this,24),dp(this,34));
        scroll.addView(root,new ScrollView.LayoutParams(ScrollView.LayoutParams.MATCH_PARENT,ScrollView.LayoutParams.WRAP_CONTENT));
        TextView brand=text("NOVA AI",14,accent); brand.setLetterSpacing(.18f); root.addView(brand);
        TextView title=text("Nova Control",32,primary); title.setPadding(0,dp(this,8),0,dp(this,4)); root.addView(title);
        TextView sub=text(subtitle,16,secondary); sub.setPadding(0,0,0,dp(this,22)); root.addView(sub);
        setContentView(scroll);
    }

    private EditText input(String hint, boolean password) {
        EditText e=new EditText(this); e.setHint(hint); e.setHintTextColor(secondary); e.setTextColor(primary); e.setSingleLine(true); e.setPadding(dp(this,14),dp(this,12),dp(this,14),dp(this,12));
        e.setInputType(password ? InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD : InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS); return e;
    }

    private void showLogin() {
        base("Your standalone Morley intelligence companion");
        LinearLayout card=new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(dp(this,18),dp(this,18),dp(this,18),dp(this,18)); card.setBackgroundColor(surface); root.addView(card);
        TextView h=text("Sign in to Morley",21,primary); card.addView(h);
        TextView note=text("Use your existing authorised Morley Admin account. Nova only reads data your account is already permitted to read; protected writes remain unavailable here.",14,secondary); note.setPadding(0,dp(this,8),0,dp(this,8)); card.addView(note);
        EditText email=input("Email",false), password=input("Password",true); card.addView(email); card.addView(password);
        TextView challengeStatus=text("Security check loading…",13,secondary); challengeStatus.setPadding(0,dp(this,8),0,dp(this,4)); card.addView(challengeStatus);
        Button signIn=button("Complete security check"); signIn.setEnabled(false);
        captchaChallenge=new CaptchaChallenge(this,(ready,message)->{challengeStatus.setText(message);challengeStatus.setTextColor(ready?success:secondary);signIn.setEnabled(ready);signIn.setText(ready?"Sign in securely":"Complete security check");});
        card.addView(captchaChallenge.view());
        TextView status=text("",13,secondary); card.addView(status);
        card.addView(signIn);
        signIn.setOnClickListener(v->{
            String e=email.getText().toString().trim(),p=password.getText().toString();
            if(e.isEmpty()||p.isEmpty()){status.setText("Enter your email and password.");return;}
            if(captchaChallenge==null||!captchaChallenge.isReady()){status.setText("Complete the security check first.");return;}
            String captchaToken=captchaChallenge.token();
            signIn.setEnabled(false); status.setTextColor(secondary); status.setText("Verifying your authorised Morley account…");
            worker.execute(()->{try{api.signIn(e,p,captchaToken);runOnUiThread(this::showWorkspace);}catch(Exception ex){runOnUiThread(()->{status.setText(ex.getMessage());status.setTextColor(danger);if(captchaChallenge!=null)captchaChallenge.reset();});}});
        });
        addOtaSection();
        footer();
    }

    private void showWorkspace() {
        base("Live Morley intelligence • signed in as " + api.signedInEmail());
        LinearLayout card=new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(dp(this,18),dp(this,18),dp(this,18),dp(this,18)); card.setBackgroundColor(surface); root.addView(card);
        TextView s=text("●  Nova intelligence online",20,success); card.addView(s);
        card.addView(text("Authorised read-only intelligence is active. Guardian approvals, pricing approvals, production releases and protected repairs cannot be executed from Nova.",14,secondary));
        Button overview=button("Live business overview"); overview.setOnClickListener(v->runSummary(IntentRouter.Intent.PERFORMANCE)); margin(overview,18);
        Button guardian=button("Guardian status & approvals"); guardian.setOnClickListener(v->runSummary(IntentRouter.Intent.GUARDIAN)); margin(guardian,8);
        Button support=button("Support queue"); support.setOnClickListener(v->runSummary(IntentRouter.Intent.SUPPORT)); margin(support,8);
        Button inventory=button("Inventory health"); inventory.setOnClickListener(v->runSummary(IntentRouter.Intent.INVENTORY)); margin(inventory,8);
        Button catalogue=button("Device catalogue health"); catalogue.setOnClickListener(v->runSummary(IntentRouter.Intent.CATALOGUE)); margin(catalogue,8);
        Button learning=button("Nova learning & accuracy"); learning.setOnClickListener(v->runSummary(IntentRouter.Intent.LEARNING)); margin(learning,8);
        Button releases=button("Release & feature state"); releases.setOnClickListener(v->runSummary(IntentRouter.Intent.RELEASES)); margin(releases,8);

        TextView chatTitle=text("Ask Nova",22,primary); chatTitle.setPadding(0,dp(this,26),0,dp(this,6)); root.addView(chatTitle);
        TextView help=text("Ask naturally about sales, profit, inventory, the device catalogue, learning accuracy, Guardian, approvals, support or releases.",14,secondary); root.addView(help);
        EditText ask=input("Ask Nova…",false); ask.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES|InputType.TYPE_TEXT_FLAG_MULTI_LINE); ask.setSingleLine(false); margin(ask,10);
        Button send=button("Ask Nova"); margin(send,8); send.setOnClickListener(v->{String q=ask.getText().toString().trim();if(!q.isEmpty()){ask.setText("");answer(q);}});

        Button logout=button("Sign out of Nova"); margin(logout,24); logout.setOnClickListener(v->{api.signOut();showLogin();});
        addOtaSection(); footer();
    }

    private void answer(String question) {
        IntentRouter.Intent intent=IntentRouter.classify(question);
        if(intent==IntentRouter.Intent.GREETING){showAnswer("Hi — I’m Nova AI. I’m connected to the live Morley data your account is authorised to read.");return;}
        if(intent==IntentRouter.Intent.CAPABILITIES){showAnswer("I can summarise live sales and profitability, inventory, valuation accuracy, catalogue health, Guardian incidents and approvals, support workload, verified learning outcomes, and release/feature state. Protected actions stay behind Morley’s existing human-approval boundaries.");return;}
        if(intent==IntentRouter.Intent.UNKNOWN){showAnswer("I understand Morley operational questions by domain. Try asking about sales performance, profit, inventory health, catalogue gaps, learning accuracy, Guardian approvals, support tickets, releases, or my capabilities.");return;}
        runSummary(intent);
    }

    private void runSummary(IntentRouter.Intent intent) {
        showAnswer("Reading live Morley data…");
        worker.execute(()->{try{String result=summarise(intent);runOnUiThread(()->showAnswer(result));}catch(Exception e){runOnUiThread(()->showAnswer("I understood the request, but the authorised live source is unavailable: "+e.getMessage()+"\n\nNo protected change was made."));}});
    }

    private String summarise(IntentRouter.Intent intent) throws Exception {
        if(intent==IntentRouter.Intent.INVENTORY){
            JSONArray rows=api.inventory(); int active=0; Map<String,Integer> states=new TreeMap<>();
            for(int i=0;i<rows.length();i++){JSONObject x=rows.getJSONObject(i);String st=x.optString("status","unknown").toLowerCase(Locale.ROOT);states.put(st,states.getOrDefault(st,0)+1);if(!st.equals("sold")&&!st.equals("closed")&&!st.equals("disposed"))active++;}
            return "Inventory health\n\n"+active+" active/available items across "+rows.length()+" recent records.\n"+states;
        }
        if(intent==IntentRouter.Intent.GUARDIAN){
            JSONArray rows=api.guardian();int open=0,approvals=0,high=0;
            for(int i=0;i<rows.length();i++){JSONObject x=rows.getJSONObject(i);String st=x.optString("state").toLowerCase(Locale.ROOT);if(!st.equals("resolved")&&!st.equals("ignored")&&!st.equals("closed")){open++;if(x.optBoolean("requires_approval"))approvals++;String risk=x.optString("risk_level").toLowerCase(Locale.ROOT);if(risk.equals("high")||risk.equals("critical"))high++;}}
            return "Guardian live status\n\n"+open+" open signals • "+approvals+" requiring human approval • "+high+" high/critical.\n\nNova reports these signals but cannot approve or apply protected repairs.";
        }
        if(intent==IntentRouter.Intent.SUPPORT){
            JSONArray rows=api.support();int open=0,urgent=0,unassigned=0,overdue=0;long now=System.currentTimeMillis();
            for(int i=0;i<rows.length();i++){JSONObject x=rows.getJSONObject(i);String st=x.optString("status").toLowerCase(Locale.ROOT);if(!st.equals("resolved")&&!st.equals("closed")){open++;String p=x.optString("priority").toLowerCase(Locale.ROOT);if(p.equals("high")||p.equals("urgent"))urgent++;if(x.isNull("assigned_to")||x.optString("assigned_to").isBlank())unassigned++;String due=x.optString("sla_due_at");if(!due.isBlank()){try{if(java.time.Instant.parse(due).toEpochMilli()<now)overdue++;}catch(Exception ignored){}}}}
            return "Support workload\n\n"+open+" active tickets • "+urgent+" high/urgent • "+unassigned+" unassigned • "+overdue+" SLA overdue.";
        }
        if(intent==IntentRouter.Intent.CATALOGUE){
            JSONArray rows=api.catalogue();int missingModel=0,missingStorage=0,missingRam=0;Map<String,Integer> categories=new TreeMap<>();
            for(int i=0;i<rows.length();i++){JSONObject x=rows.getJSONObject(i);String category=x.optString("category","unknown");categories.put(category,categories.getOrDefault(category,0)+1);if(x.optString("model_number").isBlank())missingModel++;JSONArray storage=x.optJSONArray("storage_options");if(storage==null||storage.length()==0)missingStorage++;JSONArray ram=x.optJSONArray("ram_options");if("mobile_phone".equals(category)&&!"apple".equalsIgnoreCase(x.optString("brand"))&&(ram==null||ram.length()==0))missingRam++;}
            return "Device catalogue health\n\n"+rows.length()+" active devices.\nMissing model number: "+missingModel+"\nMissing storage options: "+missingStorage+"\nNon-Apple phone RAM gaps: "+missingRam+"\n\nCategories: "+categories;
        }
        if(intent==IntentRouter.Intent.LEARNING){
            JSONArray incidents=api.guardian(), repairs=api.guardianRepairs(), valuations=api.valuations();int verified=0,recurring=0,completed=0,failed=0,realised=0;double error=0;
            for(int i=0;i<incidents.length();i++){JSONObject x=incidents.getJSONObject(i);String st=x.optString("state").toLowerCase(Locale.ROOT);if(!x.isNull("verified_at")||st.equals("resolved"))verified++;if(x.optInt("occurrence_count",1)>1)recurring++;}
            for(int i=0;i<repairs.length();i++){JSONObject x=repairs.getJSONObject(i);String st=x.optString("status").toLowerCase(Locale.ROOT);if(st.equals("completed")||st.equals("verified")||st.equals("merged")||st.equals("applied")||!x.isNull("completed_at"))completed++;if(st.equals("failed")||st.equals("rejected")||st.equals("quarantined"))failed++;}
            for(int i=0;i<valuations.length();i++){JSONObject x=valuations.getJSONObject(i);if(!x.isNull("expected_profit")&&!x.isNull("actual_profit")){error+=Math.abs(x.optDouble("actual_profit")-x.optDouble("expected_profit"));realised++;}}
            String repairRate=(completed+failed)==0?"insufficient data":Math.round(100.0*completed/(completed+failed))+"%";String forecast=realised==0?"still building":String.format(Locale.US,"AUD %.2f average absolute profit forecast error",error/realised);
            return "Nova learning & accuracy\n\n"+verified+" verified Guardian outcomes • "+recurring+" recurring incidents\nRepair success: "+repairRate+"\nValuation accuracy: "+forecast+"\n\nLearning may improve context and diagnosis, but it cannot reduce risk classifications or grant Nova authority.";
        }
        if(intent==IntentRouter.Intent.RELEASES){
            JSONArray rows=api.releaseConfig();String current="unknown",minimum="unknown";
            for(int i=0;i<rows.length();i++){JSONObject x=rows.getJSONObject(i);String key=x.optString("key");Object value=x.opt("value");String shown=releaseName(value);if("current_release".equals(key))current=shown;else if("minimum_supported_version".equals(key))minimum=shown;}
            return "Release state\n\nNova installed: "+BuildConfig.VERSION_NAME+"\nMorley current release: "+current+"\nMorley minimum supported: "+minimum+"\n\nNova’s signed OTA channel is separate and remains checksum/signature guarded.";
        }
        JSONArray sales=api.sales(), valuations=api.valuations(), inventory=api.inventory();double total=0;int profitCount=0,wins=0;
        for(int i=0;i<sales.length();i++){JSONObject x=sales.getJSONObject(i);if(!x.isNull("realised_profit")){double p=x.optDouble("realised_profit",0);total+=p;profitCount++;if(p>0)wins++;}}
        int active=0;for(int i=0;i<inventory.length();i++){String st=inventory.getJSONObject(i).optString("status").toLowerCase(Locale.ROOT);if(!st.equals("sold")&&!st.equals("closed")&&!st.equals("disposed"))active++;}
        double accuracyTotal=0;int realised=0;for(int i=0;i<valuations.length();i++){JSONObject x=valuations.getJSONObject(i);if(!x.isNull("expected_profit")&&!x.isNull("actual_profit")){accuracyTotal+=Math.abs(x.optDouble("actual_profit")-x.optDouble("expected_profit"));realised++;}}
        String accuracy=realised==0?"still building":String.format(Locale.US,"AUD %.2f average absolute profit forecast error",accuracyTotal/realised);
        return String.format(Locale.US,"Morley live overview\n\n%d recent realised sales • AUD %.2f realised profit • %.0f%% profitable-sale rate\n%d active/available inventory items\nValuation accuracy: %s",sales.length(),total,profitCount==0?0:(100.0*wins/profitCount),active,accuracy);
    }

    private static String releaseName(Object value) {
        if(value instanceof JSONObject){JSONObject x=(JSONObject)value;String n=x.optString("versionName");if(!n.isBlank())return n;return x.toString();}
        String raw=value==null||value==JSONObject.NULL?"":String.valueOf(value);if(raw.startsWith("{")){try{JSONObject x=new JSONObject(raw);String n=x.optString("versionName");if(!n.isBlank())return n;}catch(Exception ignored){}}return raw.isBlank()?"unknown":raw;
    }

    private void showAnswer(String value) { TextView out=text(value,15,primary); out.setPadding(dp(this,14),dp(this,14),dp(this,14),dp(this,14)); out.setBackgroundColor(surface); margin(out,12); }

    private void addOtaSection() {
        TextView t=text("OTA updates",20,primary); t.setPadding(0,dp(this,28),0,dp(this,8)); root.addView(t);
        updateStatus=text("Secure signed update channel ready.",14,secondary); root.addView(updateStatus);
        updateButton=button(pendingUpdate==null?"Check for updates":"Install update "+pendingUpdate.versionName); margin(updateButton,10);
        updateButton.setOnClickListener(v->{if(pendingUpdate==null)updateManager.checkForUpdates();else updateManager.downloadAndInstall(pendingUpdate);});
    }

    private void footer() { TextView b=text("Backed by Nova AI",13,secondary); b.setGravity(Gravity.CENTER_HORIZONTAL); b.setPadding(0,dp(this,36),0,dp(this,8)); root.addView(b); }

    @Override public void onStatus(String message){if(updateStatus!=null)updateStatus.setText(message);if(updateButton!=null)updateButton.setEnabled(true);}
    @Override public void onUpdateAvailable(UpdateManager.UpdateInfo info){pendingUpdate=info;if(updateStatus!=null)updateStatus.setText("Nova "+info.versionName+" is available.\n"+info.notes);if(updateButton!=null){updateButton.setText("Install update "+info.versionName);updateButton.setEnabled(true);}}
    @Override public void onUpToDate(){pendingUpdate=null;if(updateStatus!=null)updateStatus.setText("Nova is up to date.");if(updateButton!=null){updateButton.setText("Check again");updateButton.setEnabled(true);}}
    @Override public void onError(String message){if(updateStatus!=null)updateStatus.setText(message);if(updateButton!=null){updateButton.setText("Retry update check");updateButton.setEnabled(true);}}
    @Override protected void onDestroy(){if(captchaChallenge!=null)captchaChallenge.destroy();worker.shutdownNow();super.onDestroy();}
}
