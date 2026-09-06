package com.buysloans.nova;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalTime;
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
        if (intent == IntentRouter.Intent.GREETING) return greeting();
        if (intent == IntentRouter.Intent.SMALL_TALK) return smallTalk(q);
        if (intent == IntentRouter.Intent.CAPABILITIES) return "I can chat normally as well as reason across live sales, profit, inventory, catalogue quality, Guardian signals, support, releases, Nova knowledge and verified learning. Ask naturally — you do not need to phrase everything like a command. Protected approvals, repairs, pricing decisions and releases still stay behind their existing human boundaries.";
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
        return conversationalFallback(q);
    }

    void resetContext() { lastIntent = IntentRouter.Intent.UNKNOWN; }

    private String greeting() {
        int hour = LocalTime.now().getHour();
        String part = hour < 12 ? "Good morning" : hour < 18 ? "Good afternoon" : "Good evening";
        return part + " — I’m Nova. I’m here and ready. We can just talk, or you can ask me about anything happening across Morley.";
    }

    private String smallTalk(String question) {
        String x = IntentRouter.normalise(question);
        if (x.contains("how are you") || x.contains("how are things") || x.contains("how s it going") || x.contains("how is it going") || x.contains("you good") || x.contains("are you okay"))
            return "I’m doing well — connected, responsive and ready to help. How are you going?";
        if (x.contains("thank") || x.equals("thanks") || x.equals("cheers")) return "Anytime. What do you want to work on next?";
        if (x.contains("who are you") || x.contains("what are you") || x.contains("your name") || x.contains("about yourself"))
            return "I’m Nova AI — your standalone Morley intelligence companion. I can have a normal conversation, remember the current topic during this session, and work with the authorised Morley data and knowledge I’m connected to.";
        if (x.contains("what are you doing")) return "Right now I’m here with you, keeping the current conversation context and standing by for whatever you want to check or work through.";
        if (x.contains("joke")) return "Here’s one: I tried to organise the device catalogue by intuition once. The model numbers filed a formal complaint.";
        if (x.contains("tired")) return "Sounds like you’ve had a long one. I can keep things simple and help you knock over the next task without making you dig through everything manually.";
        if (x.contains("bored")) return "We can fix that. Ask me something random, give me a Morley problem to solve, or tell me what you feel like building next.";
        if (x.contains("good night") || x.contains("bye") || x.contains("goodbye") || x.contains("see you")) return "See you soon. I’ll be ready when you come back.";
        if (x.contains("awesome") || x.contains("great") || x.contains("good job") || x.contains("well done") || x.equals("nice")) return "Glad that landed well. What’s next?";
        if (x.contains("are you there")) return "Yep — I’m here.";
        return "I’m here. Tell me what’s on your mind, or ask me anything you want to work through.";
    }

    private String conversationalFallback(String q) throws Exception {
        if (q.isBlank()) return "I’m here — ask me anything.";
        String x = IntentRouter.normalise(q);
        if (x.startsWith("can we ") || x.startsWith("could we ") || x.startsWith("should we ") || x.startsWith("do you think "))
            return "Yes — we can talk it through. Give me a little more detail about what you want to change or decide, and I’ll help you work through it.";
        if (x.startsWith("i think ") || x.startsWith("i feel ") || x.startsWith("i want ") || x.startsWith("i need "))
            return "Got it. Tell me a bit more and I’ll stay with the thread instead of forcing it into a Morley data category.";
        return "I can chat about that normally, but I don’t yet have enough context to give you a useful answer. Tell me a little more about what you mean and I’ll follow the conversation from there.";
    }

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
            JSONObject byDomain=live.optJSONObject("by_domain"); JSONObject knowledge=api.knowledgeSummary();
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
