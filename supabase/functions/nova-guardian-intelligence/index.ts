import "jsr:@supabase/functions-js/edge-runtime.d.ts";

const SUPABASE_URL=Deno.env.get("SUPABASE_URL")||"";
const SERVICE_ROLE=Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")||"";
const OPENROUTER_API_KEY=Deno.env.get("OPENROUTER_API_KEY")||"";
const PRIMARY_MODEL=Deno.env.get("NOVA_GUARDIAN_MODEL")||Deno.env.get("NOVA_FUSION_MODEL")||"openai/gpt-5.6-sol";
const clip=(v:unknown,n:number)=>String(v??"").replace(/Bearer\s+[A-Za-z0-9._~+\/-]+=*/gi,"Bearer [REDACTED]").replace(/(password|token|secret|authorization|cookie|credential)\s*[:=]\s*[^\s,;]+/gi,"$1=[REDACTED]").slice(0,n);
const json=(body:unknown,status=200)=>new Response(JSON.stringify(body),{status,headers:{"content-type":"application/json; charset=utf-8","cache-control":"no-store","x-content-type-options":"nosniff"}});

function authorized(req:Request){
  const token=(req.headers.get("authorization")||"").replace(/^Bearer\s+/i,"");
  return !!SERVICE_ROLE&&token===SERVICE_ROLE&&req.headers.get("x-guardian-source")==="guardian-worker";
}
function extractJson(text:string){const a=text.indexOf("{"),b=text.lastIndexOf("}");if(a<0||b<=a)throw new Error("NOVA_NO_JSON");return JSON.parse(text.slice(a,b+1));}
async function askNova(prompt:string){
  if(!OPENROUTER_API_KEY)throw new Error("NOVA_PROVIDER_NOT_CONFIGURED");
  const controller=new AbortController();const timer=setTimeout(()=>controller.abort(),24000);
  try{
    const r=await fetch("https://openrouter.ai/api/v1/chat/completions",{method:"POST",signal:controller.signal,headers:{Authorization:`Bearer ${OPENROUTER_API_KEY}`,"Content-Type":"application/json","HTTP-Referer":"https://buyshub.me","X-Title":"Morley Nova Guardian Intelligence"},body:JSON.stringify({model:PRIMARY_MODEL,temperature:.1,max_completion_tokens:2200,messages:[{role:"system",content:"You are Nova, the guarded engineering intelligence layer for Morley Guardian. Diagnose incidents and propose the narrowest evidence-based repair. You are advisory only: never claim code, database, auth/RLS, secrets, deployments, releases or production data were changed. Never authorize a repair. Guardian owns policy and approval decisions. Return JSON only."},{role:"user",content:prompt}]})});
    const data=await r.json().catch(()=>({}));if(!r.ok)throw new Error(`NOVA_PROVIDER_${r.status}:${clip(data?.error?.message||data?.message,240)}`);
    const text=String(data?.choices?.[0]?.message?.content||"").trim();if(!text)throw new Error("NOVA_EMPTY_RESPONSE");
    return {payload:extractJson(text),model:PRIMARY_MODEL,usage:data?.usage||null};
  }finally{clearTimeout(timer)}
}

Deno.serve(async(req:Request)=>{
  if(req.method!=="POST")return json({error:"method_not_allowed"},405);
  if(!authorized(req))return json({error:"guardian_internal_authorization_required"},403);
  let body:any;try{body=await req.json()}catch{return json({error:"invalid_json"},400)}
  const incident=body?.incident&&typeof body.incident==="object"?body.incident:{};
  const repo=Array.isArray(body?.repository_context)?body.repository_context.slice(0,8).map((x:any)=>({path:clip(x?.path,260),content:clip(x?.content,12000)})):[];
  const external=!!body?.external_research_enabled;
  const prompt=`Diagnose this Morley production/application incident. Return exactly these JSON keys: diagnosis_summary (<=1800 chars), proposed_action (<=3500), reproduction_summary (<=1800), test_plan (<=1800), confidence (0..1), risk_level (low|medium|high|critical), likely_files (array max 8), repair_kind (code|edge_function|migration|auth_rls|secret|destructive_schema|unknown), sensitive_change_likely (boolean), needs_external_research (boolean).\n\nRules:\n- Separate diagnosis from authorization. You cannot authorize or execute changes.\n- Treat auth, RLS, service_role/database grants, secrets, privileged functions, production schema/data and destructive operations as sensitive.\n- Edge Function source fixes are not automatically sensitive unless they alter one of those protected areas.\n- If a database change is appropriate, propose a NEW migration rather than editing an applied migration.\n- Prefer repository evidence over assumptions.\n- External research is ${external?"available to the wider Guardian workflow if needed":"disabled"}; do not invent sources.\n\nINCIDENT:\n${JSON.stringify(incident).slice(0,18000)}\n\nREAD-ONLY REPOSITORY CONTEXT:\n${JSON.stringify(repo).slice(0,65000)}`;
  try{
    const {payload,model,usage}=await askNova(prompt);
    const risk=["low","medium","high","critical"].includes(String(payload?.risk_level))?String(payload.risk_level):"medium";
    const kind=["code","edge_function","migration","auth_rls","secret","destructive_schema","unknown"].includes(String(payload?.repair_kind))?String(payload.repair_kind):"unknown";
    return json({ok:true,source:"nova",model,usage,diagnosis:{diagnosis_summary:clip(payload?.diagnosis_summary,1800),proposed_action:clip(payload?.proposed_action,3500),reproduction_summary:clip(payload?.reproduction_summary,1800),test_plan:clip(payload?.test_plan,1800),confidence:Math.max(0,Math.min(1,Number(payload?.confidence)||.5)),risk_level:risk,likely_files:Array.isArray(payload?.likely_files)?payload.likely_files.map((x:any)=>clip(x,260)).filter(Boolean).slice(0,8):[],repair_kind:kind,sensitive_change_likely:!!payload?.sensitive_change_likely,needs_external_research:!!payload?.needs_external_research}});
  }catch(e){return json({error:"nova_diagnosis_failed",detail:clip(e instanceof Error?e.message:e,500)},502)}
});
