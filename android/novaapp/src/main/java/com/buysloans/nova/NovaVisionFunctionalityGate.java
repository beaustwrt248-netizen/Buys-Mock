package com.buysloans.nova;

import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;

final class NovaVisionFunctionalityGate {
    private static final String[] IDS = {"display_touch","front_rear_cameras","charging","biometrics","audio","buttons","connectivity","battery_health"};
    private static final String[] LABELS = {"Display & touch","Front/rear cameras","Charging","Biometrics","Audio","Buttons","Connectivity","Battery health"};
    private static final WeakHashMap<NovaVisionActivity, GateState> STATES = new WeakHashMap<>();

    private NovaVisionFunctionalityGate() {}

    static void install(NovaVisionActivity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
        GateState state = STATES.computeIfAbsent(activity, key -> new GateState());
        View decor = activity.getWindow().getDecorView();
        TextView result = findResultText(decor);
        if (result != null && state.result != result) {
            state.result = result;
            result.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(Editable s) {
                    if (state.rewriting) return;
                    String value = String.valueOf(s);
                    if (value.startsWith("Analysing ")) reset(activity);
                    else applyPricingGate(state);
                }
            });
        }
        if (decor.findViewWithTag("nova-vision-functionality-gate") != null) {
            render(state);
            return;
        }
        LinearLayout root = findVisionRoot(decor);
        if (root == null) return;
        LinearLayout card = new LinearLayout(activity);
        card.setTag("nova-vision-functionality-gate");
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(activity,16),dp(activity,16),dp(activity,16),dp(activity,16));
        card.setBackgroundColor(Color.rgb(7,23,42));
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.topMargin = dp(activity,14);
        root.addView(card, Math.max(0,root.getChildCount()-1), cardLp);

        TextView eyebrow = new TextView(activity);
        eyebrow.setText("MANUAL FUNCTIONALITY GATE");
        eyebrow.setTextColor(Color.rgb(82,139,255));
        eyebrow.setTextSize(12);
        eyebrow.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        card.addView(eyebrow);
        TextView title = new TextView(activity);
        title.setText("Hidden-function checks");
        title.setTextColor(Color.rgb(239,244,255));
        title.setTextSize(19);
        title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        title.setPadding(0,dp(activity,6),0,dp(activity,4));
        card.addView(title);
        TextView boundary = new TextView(activity);
        boundary.setText("Vision cannot prove hidden functionality. Staff must mark each check Pass or Fail. Any failure forces manual pricing; pending checks keep the final offer human-gated.");
        boundary.setTextColor(Color.rgb(169,185,211));
        boundary.setTextSize(12);
        card.addView(boundary);

        for (int i=0;i<IDS.length;i++) {
            final String id=IDS[i];
            LinearLayout row = new LinearLayout(activity);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0,dp(activity,8),0,0);
            TextView label = new TextView(activity);
            label.setText(LABELS[i]);
            label.setTextColor(Color.rgb(239,244,255));
            label.setTextSize(13);
            row.addView(label,new LinearLayout.LayoutParams(0,dp(activity,46),1f));
            Button button = new Button(activity);
            button.setAllCaps(false);
            button.setTag("nova-functionality-"+id);
            button.setOnClickListener(v -> { cycle(state,id); render(state); applyPricingGate(state); });
            row.addView(button,new LinearLayout.LayoutParams(dp(activity,116),dp(activity,46)));
            card.addView(row);
        }
        TextView status = new TextView(activity);
        status.setTag("nova-functionality-status");
        status.setTextSize(12);
        status.setPadding(0,dp(activity,10),0,0);
        card.addView(status);
        render(state);
        applyPricingGate(state);
    }

    static void reset(NovaVisionActivity activity) {
        GateState state = STATES.get(activity);
        if (state == null) return;
        for (String id : IDS) state.values.put(id, Status.PENDING);
        render(state);
    }

    static void clear(NovaVisionActivity activity) { STATES.remove(activity); }

    private static void cycle(GateState state,String id) {
        Status current=state.values.get(id);
        state.values.put(id,current==Status.PENDING?Status.PASS:current==Status.PASS?Status.FAIL:Status.PENDING);
    }

    private static void render(GateState state) {
        if (state.result == null) return;
        View decor = state.result.getRootView();
        int passed=0,failed=0,pending=0;
        for (int i=0;i<IDS.length;i++) {
            Status value=state.values.get(IDS[i]);
            if (value==Status.PASS) passed++; else if (value==Status.FAIL) failed++; else pending++;
            View view=decor.findViewWithTag("nova-functionality-"+IDS[i]);
            if (view instanceof Button) {
                Button button=(Button)view;
                button.setText(value==Status.PASS?"Pass":value==Status.FAIL?"Fail":"Not tested");
                button.setTextColor(value==Status.FAIL?Color.rgb(255,103,122):value==Status.PASS?Color.rgb(130,245,190):Color.rgb(239,244,255));
            }
        }
        View statusView=decor.findViewWithTag("nova-functionality-status");
        if (statusView instanceof TextView) {
            TextView status=(TextView)statusView;
            if (failed>0) { status.setText("Manual pricing required • "+failed+" failed • "+pending+" pending"); status.setTextColor(Color.rgb(255,103,122)); }
            else if (pending>0) { status.setText("Final offer gated • "+passed+" passed • "+pending+" still untested"); status.setTextColor(Color.rgb(255,205,110)); }
            else { status.setText("Functionality complete • 8/8 passed • final offer still requires human approval"); status.setTextColor(Color.rgb(130,245,190)); }
        }
    }

    private static void applyPricingGate(GateState state) {
        TextView result=state.result;
        if (result==null) return;
        String text=String.valueOf(result.getText());
        if (!text.contains("Australian pricing intelligence")) return;
        int failed=count(state,Status.FAIL),pending=count(state,Status.PENDING);
        String next=text;
        if (failed>0) next=next.replaceAll("Suggested maximum buy: \\$[^\\n]+","Suggested maximum buy: manual pricing required because functionality failed");
        String marker="\nFunctionality gate: ";
        int markerAt=next.indexOf(marker);
        if(markerAt>=0) next=next.substring(0,markerAt);
        if(failed>0) next+=marker+failed+" failed check"+(failed==1?"":"s")+" • automatic max-buy suppressed.";
        else if(pending>0) next+=marker+pending+" check"+(pending==1?"":"s")+" still untested • final offer not ready.";
        else next+=marker+"all 8 manual checks passed • human final approval still required.";
        if(!next.equals(text)) { state.rewriting=true; result.setText(next); state.rewriting=false; }
    }

    private static int count(GateState state,Status status){int n=0;for(Status value:state.values.values())if(value==status)n++;return n;}

    private static TextView findResultText(View root) {
        if (root instanceof TextView) {
            String value=String.valueOf(((TextView)root).getText());
            if(value.startsWith("Ready. Add front")||value.startsWith("Analysing ")||value.startsWith("Nova Vision")||value.contains("Australian pricing intelligence")) return (TextView)root;
        }
        if(root instanceof ViewGroup){ViewGroup group=(ViewGroup)root;for(int i=0;i<group.getChildCount();i++){TextView found=findResultText(group.getChildAt(i));if(found!=null)return found;}}
        return null;
    }

    private static LinearLayout findVisionRoot(View root) {
        if(root instanceof ScrollView){ScrollView scroll=(ScrollView)root;if(scroll.getChildCount()>0&&scroll.getChildAt(0) instanceof LinearLayout)return(LinearLayout)scroll.getChildAt(0);}
        if(root instanceof ViewGroup){ViewGroup group=(ViewGroup)root;for(int i=0;i<group.getChildCount();i++){LinearLayout found=findVisionRoot(group.getChildAt(i));if(found!=null)return found;}}
        return null;
    }

    private static int dp(NovaVisionActivity activity,int value){return Math.round(value*activity.getResources().getDisplayMetrics().density);}

    private enum Status { PENDING, PASS, FAIL }
    private static final class GateState {
        final Map<String,Status> values=new LinkedHashMap<>();
        TextView result;
        boolean rewriting;
        GateState(){for(String id:IDS)values.put(id,Status.PENDING);}
    }
}
