package com.buysloans.nova;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

final class NovaAssistantEngine {
    private final NovaApiClient api;
    private IntentRouter.Intent lastIntent = IntentRouter.Intent.UNKNOWN;

    NovaAssistantEngine(NovaApiClient api) { this.api = api; }

    String answer(String question) throws Exception {
        String q = question == null ? "" : question.trim();
        IntentRouter.Intent intent = IntentRouter.classify(q);
        if (intent == IntentRouter.Intent.GREETING) return "Hi — I’m Nova AI. I’m connected to Morley’s live authorised data, knowledge and verified learning sources.";
        if (intent == IntentRouter.Intent.CAPABILITIES) return "I can reason across live sales, profit, inventory, catalogue quality, Guardian signals, support, releases, Nova knowledge and verified learning. I keep protected approvals, repairs, pricing decisions and releases behind their existing human boundaries.";
        if (intent == IntentRouter.Intent.UNKNOWN && IntentRouter.isFollowUp(q) && lastIntent != IntentRouter.Intent.UNKNOWN) intent = lastIntent;
        if (intent == IntentRouter.Intent.ATTENTION) {
            lastIntent = intent;
            return attention();
        }
        if (intent != IntentRouter.Intent.UNKNOWN) {
            lastIntent = intent;
            return summarise(intent);
        }
        String knowledge = knowledgeAnswer(q);
        if (knowledge != null) return knowledge;
        return "I couldn’t match that to a live Morley domain or a stored Nova knowledge item. Try asking what needs attention, or ask about sales, profit, inventory, catalogue gaps, Guardian, support, releases, learning, or a specific Morley process.";
    }

    void resetContext() { lastIntent = IntentRouter.Intent.UNKNOWN; }

    String attention() throws Exception {
        JSONArray incidents = api.guardian(), support = api.support(), catalogue = api.catalogue();
        int approvals=0, high=0, urgent=0, overdue=0, missingModel=0, missingStorage=0;
        long now=System.currentTimeMillis();
        for(int i=0;i<incidents.length();i++){
            JSONObject x=incidents.getJSONObject(i); String st=x.optString("state").toLowerCase(Locale.ROOT);
            if(!st.equals("resolved")&&!st.equals("ignored")&&!st.equals("closed")){
                if(x.optBoolean("requires_approval")) approvals++;
                String r=x.optString("risk_level").toLowerCase(Locale.ROOT); if(r.equals("high")||r.equals("critical")) high++;
            }
        }
        for(int i=0;i<support.length();i++){
            JSONObject x=support.getJSONObject(i); String st=x.optString("status").toLowerCase(Locale.ROOT);
            if(!st.equals("resolved")&&!st.equals("closed")){
                String p=x.optString("priority").toLowerCase(Locale.ROOT); if(p.equals("high")||p.equals("urgent")) urgent++;
                String due=x.optString("sla_due_at"); if(!due.isBlank())try{if(java.time.Instant.parse(due).toEpochMilli()<now)overdue++;}catch(Exception ignored){}
            }
        }
        for(int i=0;i<catalogue.length();i++){
            JSONObject x=catalogue.getJSONObject(i); if(x.optString("model_number").isBlank()) missingModel++;
            JSONArray s=x.optJSONArray("storage_options"); if(s==null||s.length()==0) missingStorage++;
        }
        if(approvals+high+urgent+overdue+missingModel+missingStorage==0) return "Nothing urgent is showing in the authorised live sources right now.";
        return "What needs attention\n\nGuardian: "+approvals+" awaiting approval, "+high+" high/critical open.\nSupport: "+urgent+" high/urgent, "+overdue+" SLA overdue.\nCatalogue: "+missingModel+" missing model numbers, "+missingStorage+" missing storage options.\n\nNova can identify and explain these items, but protected actions remain human-approved.";
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
            return "Guardian live status\n\n"+open+" open signals • "+approvals+" requiring human approval • "+high+" high/critical.\n\nI can explain these signals but cannot approve or apply protected repairs.";
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
            JSONObject live=api.learningSummary(); int count=live.optInt("count"), verified=live.optInt("verified_count");
            JSONObject byDomain=live.optJSONObject("by_domain");
            JSONObject knowledge=api.knowledgeSummary();
            return "Nova learning & knowledge\n\n"+verified+" verified learning experiences from "+count+" active experiences.\nKnowledge base: "+knowledge.optInt("active_count")+" active items.\nDomains: "+(byDomain==null?"{}":byDomain.toString())+"\n\nLearning improves context and diagnosis but never grants Nova extra authority.";
        }
        if(intent==IntentRouter.Intent.RELEASES){
            JSONArray rows=api.releaseConfig();String current="unknown",minimum="unknown";
            for(int i=0;i<rows.length();i++){JSONObject x=rows.getJSONObject(i);String key=x.optString("key");Object value=x.opt("value");String shown=releaseName(value);if("current_release".equals(key))current=shown;else if("minimum_supported_version".equals(key))minimum=shown;}
            return "Release state\n\nNova installed: "+BuildConfig.VERSION_NAME+"\nMorley current release: "+current+"\nMorley minimum supported: "+minimum+"\n\nNova’s signed OTA channel remains checksum/signature guarded.";
        }
        JSONArray sales=api.sales(), valuations=api.valuations(), inventory=api.inventory();double total=0;int profitCount=0,wins=0;
        for(int i=0;i<sales.length();i++){JSONObject x=sales.getJSONObject(i);if(!x.isNull("realised_profit")){double p=x.optDouble("realised_profit",0);total+=p;profitCount++;if(p>0)wins++;}}
        int active=0;for(int i=0;i<inventory.length();i++){String st=inventory.getJSONObject(i).optString("status").toLowerCase(Locale.ROOT);if(!st.equals("sold")&&!st.equals("closed")&&!st.equals("disposed"))active++;}
        double accuracyTotal=0;int realised=0;for(int i=0;i<valuations.length();i++){JSONObject x=valuations.getJSONObject(i);if(!x.isNull("expected_profit")&&!x.isNull("actual_profit")){accuracyTotal+=Math.abs(x.optDouble("actual_profit")-x.optDouble("expected_profit"));realised++;}}
        String accuracy=realised==0?"still building":String.format(Locale.US,"AUD %.2f average absolute profit forecast error",accuracyTotal/realised);
        return String.format(Locale.US,"Morley live overview\n\n%d recent realised sales • AUD %.2f realised profit • %.0f%% profitable-sale rate\n%d active/available inventory items\nValuation accuracy: %s",sales.length(),total,profitCount==0?0:(100.0*wins/profitCount),active,accuracy);
    }

    private String knowledgeAnswer(String q) throws Exception {
        if(q.isBlank()) return null;
        JSONObject result=api.knowledgeSearch(q); JSONArray items=result.optJSONArray("items");
        if(items==null||items.length()==0) return null;
        StringBuilder out=new StringBuilder("I found relevant Nova knowledge:\n\n");
        int limit=Math.min(items.length(),3);
        for(int i=0;i<limit;i++){
            JSONObject item=items.getJSONObject(i); String title=item.optString("title","Knowledge item"); String trust=item.optString("trust_level","reference"); String snippet=item.optString("snippet").trim();
            if(snippet.length()>320) snippet=snippet.substring(0,320)+"…";
            out.append("• ").append(title).append(" [").append(trust).append("]\n").append(snippet).append(i+1<limit?"\n\n":"");
        }
        out.append("\n\nThis answer is grounded in stored Morley/Nova knowledge, not a protected action.");
        return out.toString();
    }

    private static String releaseName(Object value) {
        if(value instanceof JSONObject){JSONObject x=(JSONObject)value;String n=x.optString("versionName");if(!n.isBlank())return n;return x.toString();}
        String raw=value==null||value==JSONObject.NULL?"":String.valueOf(value);if(raw.startsWith("{")){try{JSONObject x=new JSONObject(raw);String n=x.optString("versionName");if(!n.isBlank())return n;}catch(Exception ignored){}}return raw.isBlank()?"unknown":raw;
    }
}
