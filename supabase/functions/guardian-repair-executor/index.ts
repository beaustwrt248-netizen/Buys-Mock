import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const URL=Deno.env.get("SUPABASE_URL")!;
const SERVICE=Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const WRITE_TOKEN=Deno.env.get("GUARDIAN_GITHUB_WRITE_TOKEN")||"";
const REPO=Deno.env.get("GUARDIAN_GITHUB_REPO")||"beaustwrt248-netizen/Buys-Mock";
const sb=createClient(URL,SERVICE,{auth:{persistSession:false,autoRefreshToken:false}});
const ALLOWED_ORIGINS=new Set(["https://buyshub.me","https://www.buyshub.me","https://beaustwrt248-netizen.github.io"]);
const cors=(req:Request)=>{const origin=req.headers.get("origin")||"";return {"access-control-allow-origin":ALLOWED_ORIGINS.has(origin)?origin:"https://buyshub.me","access-control-allow-headers":"authorization, x-client-info, apikey, content-type","access-control-allow-methods":"POST, OPTIONS","access-control-max-age":"86400","vary":"Origin"}};
const json=(req:Request,b:unknown,s=200)=>new Response(JSON.stringify(b),{status:s,headers:{...cors(req),"content-type":"application/json; charset=utf-8","cache-control":"no-store"}});
const uuid=/^[0-9a-f-]{36}$/i;
const gh=async(path:string,init:RequestInit={})=>fetch(`https://api.github.com/repos/${REPO}${path}`,{...init,headers:{Accept:"application/vnd.github+json","X-GitHub-Api-Version":"2022-11-28",...(WRITE_TOKEN?{Authorization:`Bearer ${WRITE_TOKEN}`}:{}) ,"Content-Type":"application/json",...(init.headers||{})}});
async function activity(id:string,status:string,summary:string,detail:string,progress:number){await sb.from("guardian_activity").insert({incident_id:id,phase:"testing",status,summary,detail,visibility:"admin",progress,actor:"repair-executor"})}
async function role(token:string){const {data}=await sb.auth.getUser(token);if(!data?.user)return false;const {data:p}=await sb.from("profiles").select("role,is_enabled").eq("id",data.user.id).single();return !!p?.is_enabled&&["admin","manager"].includes(p.role)}

Deno.serve(async req=>{
 if(req.method==="OPTIONS")return new Response(null,{status:204,headers:cors(req)});
 if(req.method!=="POST")return json(req,{error:"method_not_allowed"},405);
 const token=(req.headers.get("authorization")||"").replace(/^Bearer\s+/i,"");if(!token||!(await role(token)))return json(req,{error:"admin_or_manager_required"},403);
 let body:any;try{body=await req.json()}catch{return json(req,{error:"invalid_json"},400)}
 if(body?.action==="readiness"){
  if(!WRITE_TOKEN)return json(req,{ok:true,ready:false,code:"GITHUB_WRITE_NOT_CONFIGURED",repository:REPO,detail:"Protected GitHub write credential is not configured. Candidate generation remains available; branch execution is blocked."});
  try{const r=await gh("");return json(req,{ok:true,ready:r.ok,code:r.ok?"READY":`GITHUB_AUTH_${r.status}`,repository:REPO,detail:r.ok?"Protected GitHub credential is configured and accepted for repository access. Write permission is still enforced again during isolated branch execution.":"GitHub rejected the configured credential."})}catch{return json(req,{ok:true,ready:false,code:"GITHUB_UNREACHABLE",repository:REPO,detail:"GitHub could not be reached from the repair executor."})}
 }
 const repairId=String(body?.repair_id||"");if(!uuid.test(repairId))return json(req,{error:"invalid_repair"},400);
 const {data:r,error}=await sb.from("guardian_repairs").select("id,incident_id,status,candidate_files,patch_summary,base_ref,branch_name,github_pr_number").eq("id",repairId).single();if(error||!r)return json(req,{error:"repair_not_found"},404);
 if(r.status!=="testing")return json(req,{ok:true,skipped:`repair_${r.status}`});
 if(!WRITE_TOKEN){await sb.from("guardian_repairs").update({status:"failed",last_error_code:"GITHUB_WRITE_NOT_CONFIGURED",updated_at:new Date().toISOString()}).eq("id",repairId);await sb.from("guardian_incidents").update({state:"awaiting_approval",last_error_code:"GITHUB_WRITE_NOT_CONFIGURED"}).eq("id",r.incident_id);await activity(r.incident_id,"error","Branch execution is not configured.","Set the protected GUARDIAN_GITHUB_WRITE_TOKEN secret with contents/pull-request write scope. No repository change was made.",80);return json(req,{error:"github_write_not_configured"},503)}
 if(r.branch_name||r.github_pr_number)return json(req,{ok:true,skipped:"already_created"});
 try{
  const baseRes=await gh(`/git/ref/heads/${encodeURIComponent(r.base_ref||"main")}`);if(!baseRes.ok)throw new Error(`BASE_REF_${baseRes.status}`);const base=await baseRes.json();const sha=base.object.sha;
  const branch=`guardian/repair-${String(r.incident_id).slice(0,8)}-${Date.now().toString().slice(-7)}`;
  const refRes=await gh("/git/refs",{method:"POST",body:JSON.stringify({ref:`refs/heads/${branch}`,sha})});if(!refRes.ok)throw new Error(`BRANCH_CREATE_${refRes.status}`);
  await activity(r.incident_id,"working","Isolated repair branch created.",branch,83);
  for(const f of (r.candidate_files||[])){
    const path=String(f.path||"");const current=await gh(`/contents/${path}?ref=${encodeURIComponent(r.base_ref||"main")}`);if(!current.ok)throw new Error(`FILE_LOOKUP_${current.status}:${path}`);const c=await current.json();
    const put=await gh(`/contents/${path}`,{method:"PUT",body:JSON.stringify({message:`Guardian repair: ${r.patch_summary||"approved candidate"}`,content:btoa(unescape(encodeURIComponent(String(f.content||"")))),sha:c.sha,branch})});if(!put.ok)throw new Error(`FILE_WRITE_${put.status}:${path}`);
  }
  const pr=await gh("/pulls",{method:"POST",body:JSON.stringify({title:`Guardian repair: ${r.patch_summary||String(r.incident_id).slice(0,8)}`,head:branch,base:r.base_ref||"main",draft:true,body:`Protected Guardian repair candidate for incident ${r.incident_id}.\n\nThis PR was created only after two Admin/Manager approvals. It must pass repository checks and remains human-merge-only.`})});if(!pr.ok)throw new Error(`PR_CREATE_${pr.status}`);const p=await pr.json();
  await sb.from("guardian_repairs").update({branch_name:branch,github_pr_number:p.number,github_pr_url:p.html_url,updated_at:new Date().toISOString()}).eq("id",repairId);
  await sb.from("guardian_incidents").update({github_branch:branch,github_pr_number:p.number,state:"verifying"}).eq("id",r.incident_id);
  await activity(r.incident_id,"working","Draft repair PR created; repository checks are running.",`PR #${p.number} • ${branch}`,86);
  return json(req,{ok:true,repair_id:repairId,branch,pr_number:p.number,pr_url:p.html_url});
 }catch(e){const m=String(e instanceof Error?e.message:e).slice(0,500);await sb.from("guardian_repairs").update({status:"failed",last_error_code:m,updated_at:new Date().toISOString()}).eq("id",repairId);await sb.from("guardian_incidents").update({state:"failed",last_error_code:m}).eq("id",r.incident_id);await activity(r.incident_id,"error","Isolated repair execution failed.",m,82);return json(req,{error:"repair_execution_failed",detail:m},500)}
});
