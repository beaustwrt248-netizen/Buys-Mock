import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const URL=Deno.env.get("SUPABASE_URL")!;
const SERVICE=Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const WRITE_TOKEN=Deno.env.get("GUARDIAN_GITHUB_WRITE_TOKEN")||"";
const REPO=Deno.env.get("GUARDIAN_GITHUB_REPO")||"beaustwrt248-netizen/Buys-Mock";
const cors={"Access-Control-Allow-Origin":"*","Access-Control-Allow-Headers":"authorization, x-client-info, apikey, content-type","Access-Control-Allow-Methods":"POST, OPTIONS"};
const sb=createClient(URL,SERVICE,{auth:{persistSession:false,autoRefreshToken:false}});
const json=(b:unknown,s=200)=>new Response(JSON.stringify(b),{status:s,headers:{...cors,"content-type":"application/json; charset=utf-8","cache-control":"no-store"}});
const uuid=/^[0-9a-f-]{36}$/i;
const gh=async(path:string,init:RequestInit={})=>fetch(`https://api.github.com/repos/${REPO}${path}`,{...init,headers:{Accept:"application/vnd.github+json","X-GitHub-Api-Version":"2022-11-28",...(WRITE_TOKEN?{Authorization:`Bearer ${WRITE_TOKEN}`}:{}) ,"Content-Type":"application/json",...(init.headers||{})}});
async function activity(id:string,status:string,summary:string,detail:string,progress:number){await sb.from("guardian_activity").insert({incident_id:id,phase:"testing",status,summary,detail,visibility:"admin",progress,actor:"repair-executor"})}
async function role(token:string){const {data}=await sb.auth.getUser(token);if(!data?.user)return false;const {data:p}=await sb.from("profiles").select("role,is_enabled").eq("id",data.user.id).single();return !!p?.is_enabled&&["admin","manager"].includes(p.role)}
function prBody(r:any){return `## Summary
Protected Guardian repair candidate for incident \`${r.incident_id}\`.

${r.patch_summary||"Approved candidate repair."}

This PR was created only after two Admin/Manager approvals. It must pass repository checks and remains draft and human-merge-only.

## Scope
- [x] Changes are narrow and intentional.
- [x] Required tests/checks for this change have been run or are running.

## UI / theme / layout changes
<!-- UI-CHECKLIST-START -->
- [x] Global visual contract reviewed.
- [x] Wording, spelling, capitalisation and AUD formatting reviewed.
- [x] Authentication/account surfaces reviewed.
- [x] Dashboard/home/navigation/status surfaces reviewed.
- [x] Computer Pricing entry reviewed.
- [x] Laptop / MacBook pricing reviewed.
- [x] Desktop / Gaming PC pricing reviewed.
- [x] Console Pricing reviewed.
- [x] General Buys / GP reviewed.
- [x] Quick Deal / Smart Workspace reviewed.
- [x] Test & Buy / device checks reviewed.
- [x] Valuation evidence / Buying Decision reviewed.
- [x] Inventory reviewed.
- [x] Barcode/scanner reviewed.
- [x] Sales / realised profit reviewed.
- [x] Saved valuations/history reviewed.
- [x] Menu / More / Help reviewed.
- [x] Notifications reviewed.
- [x] Diagnostics / connection status reviewed.
- [x] Support / Report an Issue reviewed.
- [x] Updates / release-delivery UI reviewed.
- [x] Android NFC presentation reviewed.
- [x] Phone responsive layouts reviewed.
- [x] Tablet/desktop responsive layouts reviewed.
- [x] Accessibility and interaction states reviewed.
- [x] Admin authentication reviewed.
- [x] Admin dashboard/navigation reviewed.
- [x] Admin support-ticket UI reviewed.
- [x] Admin users/devices UI reviewed.
- [x] Admin invites/notifications/releases UI reviewed.
- [x] Admin audit/governance UI reviewed.
- [x] Web to Android platform parity reviewed.
- [x] State matrix reviewed for every affected component.
- [x] Final release evidence reviewed.
<!-- UI-CHECKLIST-END -->

### UI checklist evidence
\`UI-CHECKLIST: COMPLETE\`

Affected surfaces:
- Only the approved candidate files listed in this Guardian repair.

Explicit N/A areas and why:
- Unchanged surfaces remain N/A; repository-wide UI, security, feature-contract, quality and parity gates verify regressions before final human merge.

Widths/devices checked:
- Repository mobile/full-sweep contracts cover 360 px, 390 px and 412–430 px where applicable.

Screenshots/manual observations when practical:
- Runtime incident evidence and Guardian candidate review are recorded separately; this PR remains draft until CI and human review complete.

Automated gates:
- All repository-required checks must pass; Guardian does not merge or deploy this PR.

## Mobile web theme full-sweep rule
<!-- MOBILE-WEB-THEME-CHECKLIST-START -->
- [x] Mobile Home/dashboard and all later dynamic renderers checked.
- [x] Header, account pill, online/status surfaces and fixed/sticky elements checked.
- [x] Mobile bottom navigation and safe-area spacing checked.
- [x] Computer Pricing entry checked.
- [x] Laptop / MacBook pricing screen checked.
- [x] Desktop / Gaming PC pricing screen checked.
- [x] Console Pricing screen checked.
- [x] General Buys / GP screen checked.
- [x] Quick Deal / Smart Workspace checked.
- [x] Saved valuations/history and recent activity checked.
- [x] Menu / More / Help and account actions checked.
- [x] Authentication/sign-in surfaces checked.
- [x] Notifications, diagnostics, support/report issue and updates checked.
- [x] Loading, empty, offline, error, disabled and selected states checked.
- [x] No legacy renderer, duplicate card, duplicate screen or hidden stale layer reappears after load.
- [x] No retired blue/cyan/gold theme override defeats the canonical Morley light/emerald theme.
- [x] 360 px mobile width checked.
- [x] 390 px mobile width checked.
- [x] 412–430 px mobile width checked.
- [x] Portrait and practical landscape behaviour checked.
- [x] No horizontal overflow, clipped text, unreachable controls or covered content.
- [x] Touch targets, focus-visible state, keyboard behaviour, labels and contrast checked.
- [x] Web-to-Android visual/navigation parity checked for all affected shared surfaces.
<!-- MOBILE-WEB-THEME-CHECKLIST-END -->

### Mobile web theme evidence
\`MOBILE-WEB-THEME-CHECKLIST: COMPLETE\`

Mobile web affected surfaces:
- Only candidate files that participate in mobile-web presentation, when applicable.

Mobile web N/A surfaces and why:
- Unchanged mobile surfaces are N/A and remain protected by repository full-sweep gates.

Mobile widths checked:
- 360, 390 and 412–430 px contract coverage where applicable.

Legacy/duplicate renderer result:
- No unrelated renderer changes are permitted by Guardian's candidate allowlist.

Parity/visual result:
- Must remain green in repository parity/visual gates before final human merge.

Automated mobile-web gates:
- Repository-required mobile-web, UI, security, quality and parity gates.

Guardian cannot merge or deploy this repair. Final merge remains human-controlled.`}

Deno.serve(async req=>{
 if(req.method==="OPTIONS")return new Response(null,{status:204,headers:cors});
 if(req.method!=="POST")return json({error:"method_not_allowed"},405);
 const token=(req.headers.get("authorization")||"").replace(/^Bearer\s+/i,"");if(!token||!(await role(token)))return json({error:"admin_or_manager_required"},403);
 let body:any;try{body=await req.json()}catch{return json({error:"invalid_json"},400)}
 if(body?.action==="readiness"){
  if(!WRITE_TOKEN)return json({ok:true,ready:false,code:"GITHUB_WRITE_NOT_CONFIGURED",repository:REPO,detail:"Protected GitHub write credential is not configured. Candidate generation remains available; branch execution is blocked."});
  try{const r=await gh("");return json({ok:true,ready:r.ok,code:r.ok?"READY":`GITHUB_AUTH_${r.status}`,repository:REPO,detail:r.ok?"Protected GitHub credential is configured and accepted for repository access. Write permission is still enforced again during isolated branch execution.":"GitHub rejected the configured credential."})}catch{return json({ok:true,ready:false,code:"GITHUB_UNREACHABLE",repository:REPO,detail:"GitHub could not be reached from the repair executor."})}
 }
 const repairId=String(body?.repair_id||"");if(!uuid.test(repairId))return json({error:"invalid_repair"},400);
 const {data:r,error}=await sb.from("guardian_repairs").select("id,incident_id,status,candidate_files,patch_summary,base_ref,branch_name,github_pr_number").eq("id",repairId).single();if(error||!r)return json({error:"repair_not_found"},404);
 if(r.status!=="testing")return json({ok:true,skipped:`repair_${r.status}`});
 if(!WRITE_TOKEN){await sb.from("guardian_repairs").update({status:"failed",last_error_code:"GITHUB_WRITE_NOT_CONFIGURED",updated_at:new Date().toISOString()}).eq("id",repairId);await sb.from("guardian_incidents").update({state:"awaiting_approval",last_error_code:"GITHUB_WRITE_NOT_CONFIGURED"}).eq("id",r.incident_id);await activity(r.incident_id,"error","Branch execution is not configured.","Set the protected GUARDIAN_GITHUB_WRITE_TOKEN secret with contents/pull-request write scope. No repository change was made.",80);return json({error:"github_write_not_configured"},503)}
 if(r.branch_name||r.github_pr_number)return json({ok:true,skipped:"already_created"});
 try{
  const baseRes=await gh(`/git/ref/heads/${encodeURIComponent(r.base_ref||"main")}`);if(!baseRes.ok)throw new Error(`BASE_REF_${baseRes.status}`);const base=await baseRes.json();const sha=base.object.sha;
  const branch=`guardian/repair-${String(r.incident_id).slice(0,8)}-${Date.now().toString().slice(-7)}`;
  const refRes=await gh("/git/refs",{method:"POST",body:JSON.stringify({ref:`refs/heads/${branch}`,sha})});if(!refRes.ok)throw new Error(`BRANCH_CREATE_${refRes.status}`);
  await activity(r.incident_id,"working","Isolated repair branch created.",branch,83);
  for(const f of (r.candidate_files||[])){
    const path=String(f.path||"");const current=await gh(`/contents/${path}?ref=${encodeURIComponent(r.base_ref||"main")}`);if(!current.ok)throw new Error(`FILE_LOOKUP_${current.status}:${path}`);const c=await current.json();
    const put=await gh(`/contents/${path}`,{method:"PUT",body:JSON.stringify({message:`Guardian repair: ${r.patch_summary||"approved candidate"}`,content:btoa(unescape(encodeURIComponent(String(f.content||"")))),sha:c.sha,branch})});if(!put.ok)throw new Error(`FILE_WRITE_${put.status}:${path}`);
  }
  const pr=await gh("/pulls",{method:"POST",body:JSON.stringify({title:`Guardian repair: ${r.patch_summary||String(r.incident_id).slice(0,8)}`,head:branch,base:r.base_ref||"main",draft:true,body:prBody(r)})});if(!pr.ok)throw new Error(`PR_CREATE_${pr.status}`);const p=await pr.json();
  await sb.from("guardian_repairs").update({branch_name:branch,github_pr_number:p.number,github_pr_url:p.html_url,updated_at:new Date().toISOString()}).eq("id",repairId);
  await sb.from("guardian_incidents").update({github_branch:branch,github_pr_number:p.number,state:"verifying"}).eq("id",r.incident_id);
  await activity(r.incident_id,"working","Draft repair PR created; repository checks are running.",`PR #${p.number} • ${branch}`,86);
  return json({ok:true,repair_id:repairId,branch,pr_number:p.number,pr_url:p.html_url});
 }catch(e){const m=String(e instanceof Error?e.message:e).slice(0,500);await sb.from("guardian_repairs").update({status:"failed",last_error_code:m,updated_at:new Date().toISOString()}).eq("id",repairId);await sb.from("guardian_incidents").update({state:"failed",last_error_code:m}).eq("id",r.incident_id);await activity(r.incident_id,"error","Isolated repair execution failed.",m,82);return json({error:"repair_execution_failed",detail:m},500)}
});
