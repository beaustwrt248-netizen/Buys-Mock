import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const PROJECT_URL=Deno.env.get("SUPABASE_URL")!;
const SERVICE_ROLE_KEY=Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const GITHUB_TOKEN=Deno.env.get("GUARDIAN_GITHUB_READ_TOKEN")||"";
const GITHUB_REPO=Deno.env.get("GUARDIAN_GITHUB_REPO")||"beaustwrt248-netizen/Buys-Mock";
const GITHUB_REF=Deno.env.get("GUARDIAN_GITHUB_REF")||"main";
const sb=createClient(PROJECT_URL,SERVICE_ROLE_KEY,{auth:{persistSession:false,autoRefreshToken:false}});
const json=(body:unknown,status=200)=>new Response(JSON.stringify(body),{status,headers:{"content-type":"application/json; charset=utf-8","cache-control":"no-store","x-content-type-options":"nosniff"}});
const uuid=/^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
const clip=(v:unknown,n:number)=>String(v??"").replace(/Bearer\s+[A-Za-z0-9._~+\/-]+=*/gi,"Bearer [REDACTED]").replace(/(password|token|secret|authorization|cookie|credential)\s*[:=]\s*[^\s,;]+/gi,"$1=[REDACTED]").slice(0,n);

async function activity(incidentId:string,phase:string,status:string,summary:string,detail:string|null,progress:number,visibility="admin",actor="worker"){
  await sb.from("guardian_activity").insert({incident_id:incidentId,phase,status,summary:clip(summary,500),detail:detail?clip(detail,3000):null,visibility,progress,actor});
}
function evidence(input:any,triage:any={}){return `${input?.category||""} ${input?.classification||""} ${input?.description||""} ${input?.diagnostic_message||""} ${JSON.stringify(input?.diagnostics||input?.diagnostic_metadata||{})} ${triage?.diagnosis_summary||""} ${triage?.proposed_action||""} ${(triage?.likely_files||[]).join(" ")}`.toLowerCase()}
function guardianPolicy(input:any,triage:any){
  const text=evidence(input,triage);
  const protectedScope=!!triage?.sensitive_change_likely||["auth_rls","secret","destructive_schema","migration"].includes(String(triage?.repair_kind||""))||/\b(service_role|row level security|\brls\b|grant\s|revoke\s|secret|credential|private key|security definer|production schema|drop table|drop schema|alter role|create role|payment)\b/i.test(text);
  const confidence=Math.max(0,Math.min(1,Number(triage?.confidence)||.5));
  const risk=protectedScope&&["low","medium"].includes(String(triage?.risk_level))?"high":(["low","medium","high","critical"].includes(String(triage?.risk_level))?String(triage.risk_level):"medium");
  return {protected_scope:protectedScope,requires_approval:true,auto_fix_eligible:!protectedScope&&confidence>=.8&&risk!=="critical",risk_level:risk};
}
function fallback(input:any){
  const category=String(input?.category??input?.classification??"other").toLowerCase();
  const description=String(input?.description??input?.diagnostic_message??"").trim();
  const diagnostics=input?.diagnostics&&typeof input.diagnostics==="object"?input.diagnostics:(input?.diagnostic_metadata&&typeof input.diagnostic_metadata==="object"?input.diagnostic_metadata:{});
  const privileged=["account","auth","security","payment"].includes(category);
  if(privileged)return{risk_level:"high",confidence:.55,diagnosis_summary:"Guardian identified a privileged incident. Automated production changes remain blocked by policy.",proposed_action:"Admin/Manager review is required. Continue evidence collection and reproduction before any protected change.",reproduction_summary:"Privileged incident isolated for controlled investigation.",test_plan:"Reproduce without changing authentication, authorization, secrets or production data.",repair_kind:"unknown",sensitive_change_likely:true,last_error_code:"PRIVILEGED_REVIEW_REQUIRED",likely_files:[]};
  if(!Object.keys(diagnostics).length&&description.length<12)return{risk_level:"medium",confidence:.2,diagnosis_summary:"Guardian does not yet have enough evidence for a reliable diagnosis.",proposed_action:"Collect screen context, app version, route, device information and relevant errors, then retry.",reproduction_summary:"Insufficient evidence to reproduce safely.",test_plan:"Gather diagnostics before running repair tests.",repair_kind:"unknown",sensitive_change_likely:false,last_error_code:"INSUFFICIENT_DIAGNOSTICS",likely_files:[]};
  return{risk_level:"medium",confidence:.5,diagnosis_summary:`Guardian found enough context to prepare an engineering diagnosis for this ${category} incident.`,proposed_action:"Inspect the most relevant current source files, reproduce the failure, prepare the narrowest candidate change and validate it through the protected test pipeline.",reproduction_summary:"Initial evidence is sufficient for targeted reproduction.",test_plan:"Run the smallest relevant automated checks first, then the repository quality gates before approval.",repair_kind:"unknown",sensitive_change_likely:false,last_error_code:null,likely_files:[]};
}
function keywords(input:any){return Array.from(new Set(`${input?.category||""} ${input?.classification||""} ${input?.subject||""} ${input?.description||""} ${input?.route||""} ${input?.diagnostic_kind||""}`.toLowerCase().split(/[^a-z0-9_-]+/).filter((x:string)=>x.length>=4))).slice(0,14)}
function safePath(path:string){return /\.(html|css|js|ts|tsx|jsx|kt|kts|java|json|yml|yaml|md|sql)$/i.test(path)&&!/(secret|credential|\.env|keystore|private[-_]?key|service[-_]?account)/i.test(path)}
async function repoContext(input:any,enabled:boolean){
  if(!enabled)return[];const headers:Record<string,string>={Accept:"application/vnd.github+json","X-GitHub-Api-Version":"2022-11-28","User-Agent":"morley-guardian-readonly"};if(GITHUB_TOKEN)headers.Authorization=`Bearer ${GITHUB_TOKEN}`;
  const treeRes=await fetch(`https://api.github.com/repos/${GITHUB_REPO}/git/trees/${encodeURIComponent(GITHUB_REF)}?recursive=1`,{headers});if(!treeRes.ok)return[];const tree=await treeRes.json();const ks=keywords(input);
  const candidates=(tree.tree||[]).filter((x:any)=>x.type==="blob"&&safePath(String(x.path||""))&&Number(x.size||0)<=100000).map((x:any)=>({path:String(x.path),score:ks.reduce((s:number,k:string)=>s+(String(x.path).toLowerCase().includes(k)?3:0),0)+(String(x.path).startsWith("admin/")&&String(input?.route||"").includes("admin")?2:0)})).filter((x:any)=>x.score>0).sort((a:any,b:any)=>b.score-a.score).slice(0,8);
  const out:any[]=[];for(const c of candidates){const r=await fetch(`https://api.github.com/repos/${GITHUB_REPO}/contents/${c.path}?ref=${encodeURIComponent(GITHUB_REF)}`,{headers});if(!r.ok)continue;const d=await r.json();if(d.encoding!=="base64"||!d.content)continue;try{const bytes=Uint8Array.from(atob(String(d.content).replace(/\n/g,"")),c=>c.charCodeAt(0));out.push({path:c.path,content:clip(new TextDecoder().decode(bytes),12000)})}catch{}}return out;
}
async function novaDiagnosis(input:any,repo:any[],enabled:boolean,external:boolean){
  if(!enabled)return null;
  const r=await fetch(`${PROJECT_URL}/functions/v1/nova-guardian-intelligence`,{method:"POST",headers:{Authorization:`Bearer ${SERVICE_ROLE_KEY}`,"Content-Type":"application/json","x-guardian-source":"guardian-worker"},body:JSON.stringify({incident:input,repository_context:repo,external_research_enabled:external})});
  const data=await r.json().catch(()=>({}));if(!r.ok||!data?.ok)throw new Error(`NOVA_${r.status}:${clip(data?.detail||data?.error,400)}`);return data.diagnosis;
}

Deno.serve(async(req:Request)=>{
  if(req.method!=="POST")return json({error:"method_not_allowed"},405);let body:any;try{body=await req.json()}catch{return json({error:"invalid_json"},400)}
  const incidentId=String(body?.incident_id??"").trim(),dispatchToken=String(body?.dispatch_token??"").trim();if(!uuid.test(incidentId)||!uuid.test(dispatchToken))return json({error:"invalid_dispatch"},400);
  const {data:settings,error:settingsError}=await sb.from("guardian_settings").select("enabled,kill_switch,operating_mode,ai_enabled,repository_read_enabled,external_research_enabled,agent_model").eq("singleton",true).single();if(settingsError)return json({error:"settings_lookup_failed"},500);if(!settings?.enabled||settings?.kill_switch)return json({ok:true,skipped:"guardian_disabled"});
  const {data:incident,error:incidentError}=await sb.from("guardian_incidents").select("id,ticket_id,state,attempt_count,dispatch_token,source,classification,route,diagnostic_kind,diagnostic_message,diagnostic_metadata,app_version").eq("id",incidentId).single();if(incidentError||!incident||String(incident.dispatch_token)!==dispatchToken)return json({error:"dispatch_rejected"},401);if(incident.state!=="queued")return json({ok:true,skipped:`state_${incident.state}`});
  const {data:claimed,error:claimError}=await sb.from("guardian_incidents").update({state:"diagnosing",attempt_count:Number(incident.attempt_count??0)+1,last_worker_at:new Date().toISOString(),worker_version:"guardian-nova-v4",last_error_code:null,dispatch_token:crypto.randomUUID()}).eq("id",incidentId).eq("state","queued").eq("dispatch_token",dispatchToken).select("id").maybeSingle();if(claimError)return json({error:"claim_failed"},500);if(!claimed)return json({ok:true,skipped:"already_claimed"});
  const visibility=incident.ticket_id?"user":"admin";await activity(incidentId,"diagnosing","working","Guardian started a Nova-assisted diagnosis.","Guardian owns evidence, policy and approvals; Nova supplies engineering intelligence only.",25,visibility);
  let ticket:any=null;if(incident.ticket_id){const r=await sb.from("support_tickets").select("id,category,subject,description,priority,app_version,device_model,android_version,diagnostics,created_at").eq("id",incident.ticket_id).single();if(!r.error)ticket=r.data}
  const input=ticket?{...ticket,classification:incident.classification,route:incident.route}:{category:incident.classification,classification:incident.classification,description:incident.diagnostic_message,diagnostic_kind:incident.diagnostic_kind,diagnostic_message:incident.diagnostic_message,diagnostics:incident.diagnostic_metadata,diagnostic_metadata:incident.diagnostic_metadata,route:incident.route,app_version:incident.app_version,source:incident.source};
  await activity(incidentId,"inspecting","working","Inspecting relevant source context.",settings.repository_read_enabled!==false?"Guardian is using read-only repository access. No code changes are permitted in diagnosis.":"Repository inspection is disabled in Guardian controls.",38,visibility);
  let repo:any[]=[];try{repo=await repoContext(input,settings.repository_read_enabled!==false)}catch{}if(repo.length)await activity(incidentId,"inspecting","success","Relevant source files identified.",repo.map(x=>x.path).join(" • "),45,"admin");
  let triage:any=null;try{await activity(incidentId,"diagnosing","working","Nova engineering intelligence is running.","Nova diagnoses; Guardian independently applies protected-scope policy.",50,visibility);triage=await novaDiagnosis(input,repo,settings.ai_enabled!==false,!!settings.external_research_enabled)}catch(e){await activity(incidentId,"diagnosing","warning","Nova diagnosis was unavailable; Guardian continued safely.",clip(e instanceof Error?e.message:e,500),52,"admin")}
  triage=triage||fallback(input);const policy=guardianPolicy(input,triage);
  await activity(incidentId,"policy","success",policy.protected_scope?"Guardian classified this as protected scope.":"Guardian classified the proposed repair scope.",policy.protected_scope?"Auth/RLS/privileges/secrets/database migrations or other sensitive production changes remain approval-gated.":"Non-sensitive code/Edge Function candidates may be prepared, but repository writes still remain approval-gated.",58,"admin");
  await activity(incidentId,"preparing_fix","working","Guardian prepared the diagnosis and repair plan.",triage.proposed_action,62,visibility);
  const state=triage.last_error_code==="INSUFFICIENT_DIAGNOSTICS"?"failed":"proposed";
  const {error:updateError}=await sb.from("guardian_incidents").update({state,risk_level:policy.risk_level,confidence:triage.confidence,diagnosis_summary:triage.diagnosis_summary,proposed_action:triage.proposed_action,reproduction_summary:triage.reproduction_summary,test_plan:triage.test_plan,auto_fix_eligible:policy.auto_fix_eligible,requires_approval:true,last_error_code:triage.last_error_code||null,classification:String(input.category??input.classification??"other"),worker_version:"guardian-nova-v4",last_worker_at:new Date().toISOString()}).eq("id",incidentId);if(updateError)return json({error:"incident_update_failed"},500);
  await activity(incidentId,"awaiting_approval","waiting","Diagnosis complete; repository and production actions remain approval-gated.",`Nova confidence ${Math.round(Number(triage.confidence||0)*100)}%. Guardian risk ${policy.risk_level}. Protected scope: ${policy.protected_scope?"yes":"no"}.`,65,visibility);
  return json({ok:true,incident_id:incidentId,state,risk_level:policy.risk_level,confidence:triage.confidence,repository_files:repo.map(x=>x.path),nova_used:!!triage&&settings.ai_enabled!==false,protected_scope:policy.protected_scope,auto_fix_eligible:policy.auto_fix_eligible,requires_approval:true});
});
