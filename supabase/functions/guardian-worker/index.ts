import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const PROJECT_URL = Deno.env.get("SUPABASE_URL")!;
const SERVICE_ROLE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const sb = createClient(PROJECT_URL, SERVICE_ROLE_KEY, { auth: { persistSession: false, autoRefreshToken: false } });
const json = (body: unknown, status = 200) => new Response(JSON.stringify(body), { status, headers: { "content-type":"application/json; charset=utf-8", "cache-control":"no-store", "x-content-type-options":"nosniff" } });

function classify(ticket: any) {
  const category = String(ticket?.category ?? "other").toLowerCase();
  const description = String(ticket?.description ?? "").trim();
  const diagnostics = ticket?.diagnostics && typeof ticket.diagnostics === "object" ? ticket.diagnostics : {};
  const hasDiagnostics = Object.keys(diagnostics).length > 0;
  const hasUsefulDescription = description.length >= 20;
  const privileged = ["account","auth","security","payment"].includes(category);
  if (privileged) return { state:"awaiting_approval", risk_level:"high", confidence:0.55, diagnosis_summary:"Guardian identified a privileged support category. Automated production changes are blocked by policy.", proposed_action:"Admin/Manager review required. Collect diagnostics and reproduce before any privileged change.", auto_fix_eligible:false, requires_approval:true, last_error_code:"PRIVILEGED_REVIEW_REQUIRED" };
  if (!hasDiagnostics && !hasUsefulDescription) return { state:"failed", risk_level:"medium", confidence:0.2, diagnosis_summary:"Guardian could not determine a reliable root cause from the current report because diagnostic evidence is missing or too limited.", proposed_action:"Collect screen/context, device/app version details, relevant logs or stack trace, and clear reproduction steps; then requeue the incident.", auto_fix_eligible:false, requires_approval:true, last_error_code:"INSUFFICIENT_DIAGNOSTICS" };
  return { state:"proposed", risk_level:"medium", confidence:hasDiagnostics?0.72:0.48, diagnosis_summary:`Guardian triaged this ${category} incident and found enough context for engineering review, but not enough evidence for an autonomous production fix.`, proposed_action:"Correlate the report with recent telemetry and current main code, reproduce where possible, then prepare a narrow repair PR if the evidence supports one.", auto_fix_eligible:false, requires_approval:true, last_error_code:null };
}

Deno.serve(async (req: Request) => {
  if (req.method !== "POST") return json({error:"method_not_allowed"},405);
  let body:any; try { body = await req.json(); } catch { return json({error:"invalid_json"},400); }
  const incidentId = String(body?.incident_id ?? "").trim();
  if (!/^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(incidentId)) return json({error:"invalid_incident_id"},400);
  const {data:settings,error:settingsError}=await sb.from("guardian_settings").select("enabled").eq("singleton",true).single();
  if(settingsError) return json({error:"settings_lookup_failed"},500);
  if(!settings?.enabled) return json({ok:true,skipped:"guardian_disabled"});
  const {data:incident,error:incidentError}=await sb.from("guardian_incidents").select("id,ticket_id,state,attempt_count").eq("id",incidentId).single();
  if(incidentError||!incident) return json({error:"incident_not_found"},404);
  if(incident.state!=="queued") return json({ok:true,skipped:`state_${incident.state}`});
  const {data:claimed,error:claimError}=await sb.from("guardian_incidents").update({state:"diagnosing",attempt_count:Number(incident.attempt_count??0)+1,last_worker_at:new Date().toISOString(),worker_version:"guardian-live-v1",last_error_code:null}).eq("id",incidentId).eq("state","queued").select("id").maybeSingle();
  if(claimError) return json({error:"claim_failed"},500);
  if(!claimed) return json({ok:true,skipped:"already_claimed"});
  const {data:ticket,error:ticketError}=await sb.from("support_tickets").select("id,category,subject,description,priority,app_version,device_model,android_version,diagnostics,created_at").eq("id",incident.ticket_id).single();
  if(ticketError||!ticket){await sb.from("guardian_incidents").update({state:"failed",risk_level:"medium",confidence:0.1,diagnosis_summary:"Guardian could not load the linked support ticket.",proposed_action:"Verify the incident-to-ticket linkage and retry.",requires_approval:true,auto_fix_eligible:false,last_error_code:"TICKET_LOOKUP_FAILED",last_worker_at:new Date().toISOString()}).eq("id",incidentId);return json({error:"ticket_lookup_failed"},500);}
  const triage=classify(ticket);
  const {error:updateError}=await sb.from("guardian_incidents").update({...triage,classification:String(ticket.category??"other"),worker_version:"guardian-live-v1",last_worker_at:new Date().toISOString()}).eq("id",incidentId);
  if(updateError) return json({error:"incident_update_failed"},500);
  return json({ok:true,incident_id:incidentId,state:triage.state,risk_level:triage.risk_level});
});
