import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "jsr:@supabase/supabase-js@2";

const SUPABASE_URL=Deno.env.get('SUPABASE_URL')!;
const SERVICE_ROLE=Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!;
const admin=createClient(SUPABASE_URL,SERVICE_ROLE,{auth:{persistSession:false,autoRefreshToken:false}});
const cors={'Access-Control-Allow-Origin':'*','Access-Control-Allow-Headers':'authorization, content-type','Access-Control-Allow-Methods':'POST, OPTIONS'};
function reply(body:unknown,status=200){return new Response(JSON.stringify(body),{status,headers:{...cors,'Content-Type':'application/json; charset=utf-8','Cache-Control':'no-store','X-Content-Type-Options':'nosniff'}})}
async function requireAdmin(req:Request){
  const h=req.headers.get('Authorization')||'';
  if(!h.startsWith('Bearer '))throw new Error('AUTH_REQUIRED');
  const token=h.slice(7).trim();
  const {data,error}=await admin.auth.getUser(token);
  if(error||!data.user)throw new Error('AUTH_REQUIRED');
  const {data:p,error:pe}=await admin.from('profiles').select('role,is_enabled').eq('id',data.user.id).single();
  if(pe||!p?.is_enabled||!['admin','manager'].includes(String(p.role)))throw new Error('FORBIDDEN');
  return {id:data.user.id,role:String(p.role)};
}
async function count(table:string,apply:(q:any)=>any){const q=apply(admin.from(table).select('id',{count:'exact',head:true}));const {count,error}=await q;if(error)throw error;return Number(count||0)}
Deno.serve(async req=>{
  if(req.method==='OPTIONS')return new Response(null,{status:204,headers:cors});
  if(req.method!=='POST')return reply({error:'Method not allowed'},405);
  try{
    const user=await requireAdmin(req);
    const since24=new Date(Date.now()-86400000).toISOString();
    const since7d=new Date(Date.now()-7*86400000).toISOString();
    const [novaPending,novaHigh,novaRecentRuns,priceZeroOrNegative,priceLargeRecent,openRecovery,criticalRecovery]=await Promise.all([
      count('nova_catalog_audit_findings',q=>q.in('status',['open','pending','proposed','awaiting_approval'])),
      count('nova_catalog_audit_findings',q=>q.in('status',['open','pending','proposed','awaiting_approval']).in('severity',['high','critical'])),
      count('nova_catalog_audit_runs',q=>q.gte('started_at',since24)),
      count('device_buy_prices',q=>q.eq('is_active',true).lte('price_aud',0)),
      count('device_buy_price_history',q=>q.gte('changed_at',since7d).or('new_price_aud.gt.5000,new_price_aud.lt.0')),
      count('recovery_health_findings',q=>q.eq('status','open')),
      count('recovery_health_findings',q=>q.eq('status','open').in('severity',['high','critical']))
    ]);
    return reply({
      generated_at:new Date().toISOString(),
      scope:'aggregate_only',
      actor_role:user.role,
      nova:{pending_findings:novaPending,high_critical_pending:novaHigh,runs_last_24h:novaRecentRuns,raw_evidence_exposed:false},
      pricing:{active_zero_or_negative:priceZeroOrNegative,extreme_changes_last_7d:priceLargeRecent,raw_prices_exposed:false,automatic_price_changes:false},
      recovery:{open_findings:openRecovery,high_critical_open:criticalRecovery,raw_user_backups_exposed:false,automatic_restore:false},
      controls:{authority_expansion:false,destructive_actions:false,approval_bypass:false}
    });
  }catch(e){
    const m=e instanceof Error?e.message:String(e);
    if(m==='AUTH_REQUIRED')return reply({error:'Authentication required'},401);
    if(m==='FORBIDDEN')return reply({error:'Admin or manager access required'},403);
    console.error('guardian-sensitive-readiness',m);
    return reply({error:'Guardian sensitive readiness check failed'},500);
  }
});
