import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "jsr:@supabase/supabase-js@2";

const SUPABASE_URL=Deno.env.get('SUPABASE_URL')||'';
const SERVICE_ROLE=Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')||'';
const ADMIN_ORIGINS=new Set(['https://buyshub.me','https://www.buyshub.me','https://beaustwrt248-netizen.github.io']);
const admin=createClient(SUPABASE_URL,SERVICE_ROLE,{auth:{persistSession:false,autoRefreshToken:false}});
const clean=(v:unknown,max=1000)=>String(v??'').trim().replace(/\s+/g,' ').slice(0,max);
const daysBetween=(a:unknown,b:unknown)=>{const x=new Date(String(a||'')).getTime(),y=new Date(String(b||'')).getTime();return Number.isFinite(x)&&Number.isFinite(y)?Math.max(0,Math.round((y-x)/86400000)):null};

function corsHeaders(req:Request){
  const origin=req.headers.get('Origin')||'';
  const allowed=ADMIN_ORIGINS.has(origin)?origin:'https://buyshub.me';
  return {'Content-Type':'application/json','Access-Control-Allow-Origin':allowed,'Access-Control-Allow-Headers':'authorization, x-client-info, apikey, content-type','Access-Control-Allow-Methods':'POST, OPTIONS','Vary':'Origin','Cache-Control':'no-store','X-Content-Type-Options':'nosniff'};
}

async function authorise(req:Request){
  if(!SUPABASE_URL||!SERVICE_ROLE) throw new Error('Nova learning backend configuration unavailable');
  const token=(req.headers.get('Authorization')||'').replace(/^Bearer\s+/i,'');
  if(!token) return {error:'Authentication required',status:401} as const;
  const {data:{user},error}=await admin.auth.getUser(token);
  if(error||!user) return {error:'Invalid session',status:401} as const;
  const {data:caller,error:profileError}=await admin.from('profiles').select('id,role,is_enabled').eq('id',user.id).maybeSingle();
  if(profileError) throw profileError;
  if(!caller?.is_enabled||!['admin','manager'].includes(caller.role)) return {error:'Admin or Manager access required',status:403} as const;
  return {user,caller} as const;
}

async function upsertExperience(row:any){
  const payload={...row,summary:clean(row.summary,1200),lesson_key:clean(row.lesson_key,240),lesson_type:clean(row.lesson_type,80),source_type:clean(row.source_type,80),source_id:row.source_id?clean(row.source_id,160):null,outcome:row.outcome?clean(row.outcome,120):null,updated_at:new Date().toISOString()};
  const {error}=await admin.from('nova_learning_experiences').upsert(payload,{onConflict:'domain,lesson_key'});
  if(error) throw error;
}

async function harvest(userId:string){
  const [incidentsRes,repairsRes,valsRes,inventoryRes,salesRes]=await Promise.all([
    admin.from('guardian_incidents').select('id,fingerprint,state,risk_level,classification,confidence,diagnosis_summary,proposed_action,resolution_summary,occurrence_count,verified_at,last_seen_at,updated_at').order('updated_at',{ascending:false}).limit(400),
    admin.from('guardian_repairs').select('id,incident_id,status,patch_summary,test_results,generated_at,tested_at,completed_at,updated_at').order('updated_at',{ascending:false}).limit(400),
    admin.from('valuation_history').select('id,item_type,item_summary,item_grade,status,expected_profit,actual_profit,bought_price,sold_price,confidence,created_at,updated_at').order('created_at',{ascending:false}).limit(400),
    admin.from('inventory_items').select('id,valuation_id,device_catalog_id,item_type,item_summary,model_number,storage,item_grade,acquired_price,expected_sale_price,status,acquired_at,listed_at,updated_at').order('acquired_at',{ascending:false}).limit(500),
    admin.from('sales_records').select('id,inventory_item_id,valuation_id,acquired_cost,sold_price,fees,other_costs,realised_profit,sales_channel,sold_at,created_at').order('sold_at',{ascending:false}).limit(500)
  ]);
  if(incidentsRes.error) throw incidentsRes.error;
  if(repairsRes.error) throw repairsRes.error;
  if(valsRes.error) throw valsRes.error;
  if(inventoryRes.error && inventoryRes.error.code!=='42P01') throw inventoryRes.error;
  if(salesRes.error && salesRes.error.code!=='42P01') throw salesRes.error;
  const incidents=incidentsRes.data||[],repairs=repairsRes.data||[],vals=valsRes.data||[],inventory=inventoryRes.data||[],sales=salesRes.data||[];
  const inventoryById=new Map(inventory.map((x:any)=>[String(x.id),x]));
  let written=0,inventoryOpen=0,salesRealised=0;

  for(const i of incidents){
    const state=String(i.state||'').toLowerCase(),verified=!!i.verified_at||state==='resolved',recurring=Number(i.occurrence_count||1)>1;
    if(!verified&&!recurring) continue;
    const key=`incident:${i.fingerprint||i.id}:${verified?'verified':'recurring'}`;
    const summary=verified?`${i.classification||'Guardian incident'} resolved/verified after ${i.occurrence_count||1} occurrence(s). Diagnosis: ${i.diagnosis_summary||'not recorded'}. Resolution: ${i.resolution_summary||'not recorded'}.`:`${i.classification||'Guardian incident'} has recurred ${i.occurrence_count||1} times. Diagnosis: ${i.diagnosis_summary||'not recorded'}.`;
    await upsertExperience({domain:'guardian',lesson_key:key,lesson_type:verified?'verified_outcome':'recurring_pattern',summary,source_type:'guardian_incident',source_id:String(i.id),evidence:{risk_level:i.risk_level,classification:i.classification,diagnosis_summary:i.diagnosis_summary,proposed_action:i.proposed_action,resolution_summary:i.resolution_summary,occurrence_count:i.occurrence_count},outcome:verified?'resolved':'recurring',confidence:Number(i.confidence??(verified?0.9:0.7)),verified,active:true,observed_at:i.last_seen_at||i.updated_at,created_by:userId});written++;
  }

  for(const r of repairs){
    const status=String(r.status||'').toLowerCase(),success=['completed','verified','merged','applied'].includes(status)||!!r.completed_at,failure=['failed','rejected','quarantined'].includes(status);
    if(!success&&!failure) continue;
    await upsertExperience({domain:'guardian',lesson_key:`repair:${r.id}:${success?'success':'failure'}`,lesson_type:success?'repair_success':'repair_failure',summary:`Guardian repair ${success?'succeeded':'failed'}${r.patch_summary?`: ${r.patch_summary}`:''}.`,source_type:'guardian_repair',source_id:String(r.id),evidence:{incident_id:r.incident_id,status:r.status,patch_summary:r.patch_summary,test_results:r.test_results},outcome:success?'success':'failure',confidence:success?0.95:0.9,verified:success,active:true,observed_at:r.completed_at||r.tested_at||r.updated_at,created_by:userId});written++;
  }

  for(const item of inventory){
    if(String(item.status||'').toLowerCase()==='sold') continue;
    inventoryOpen++;
    const held=daysBetween(item.acquired_at,new Date().toISOString());
    await upsertExperience({domain:'inventory',lesson_key:`inventory:${item.id}:holding`,lesson_type:'authoritative_holding',summary:`${item.item_summary||item.item_type||'Item'} is ${item.status||'in inventory'} after acquisition at AUD ${Number(item.acquired_price||0).toFixed(2)}${held==null?'':` and has been held about ${held} day(s)`}.`,source_type:'inventory_items',source_id:String(item.id),evidence:{valuation_id:item.valuation_id,device_catalog_id:item.device_catalog_id,item_type:item.item_type,model_number:item.model_number,storage:item.storage,item_grade:item.item_grade,acquired_price:item.acquired_price,expected_sale_price:item.expected_sale_price,status:item.status,acquired_at:item.acquired_at,listed_at:item.listed_at,holding_days:held},outcome:String(item.status||'holding'),confidence:0.99,verified:true,active:true,observed_at:item.updated_at||item.acquired_at,created_by:userId});written++;
  }

  for(const sale of sales){
    salesRealised++;
    const item:any=inventoryById.get(String(sale.inventory_item_id))||null,held=item?daysBetween(item.acquired_at,sale.sold_at):null,profit=Number(sale.realised_profit??(Number(sale.sold_price||0)-Number(sale.acquired_cost||0)-Number(sale.fees||0)-Number(sale.other_costs||0)));
    await upsertExperience({domain:'sales',lesson_key:`sale:${sale.id}:authoritative`,lesson_type:'authoritative_realised_sale',summary:`${item?.item_summary||'Inventory item'} sold for AUD ${Number(sale.sold_price||0).toFixed(2)} with realised profit AUD ${profit.toFixed(2)}${held==null?'':` after about ${held} day(s) held`}.`,source_type:'sales_records',source_id:String(sale.id),evidence:{inventory_item_id:sale.inventory_item_id,valuation_id:sale.valuation_id,item_type:item?.item_type,item_grade:item?.item_grade,model_number:item?.model_number,storage:item?.storage,acquired_cost:sale.acquired_cost,sold_price:sale.sold_price,fees:sale.fees,other_costs:sale.other_costs,realised_profit:profit,sales_channel:sale.sales_channel,sold_at:sale.sold_at,holding_days:held},outcome:profit>=0?'positive_realised_profit':'negative_realised_profit',confidence:0.99,verified:true,active:true,observed_at:sale.sold_at||sale.created_at,created_by:userId});written++;
  }

  for(const v of vals){
    if(v.actual_profit==null||v.expected_profit==null) continue;
    const expected=Number(v.expected_profit),actual=Number(v.actual_profit),error=actual-expected;
    await upsertExperience({domain:'valuation',lesson_key:`valuation:${v.id}:realised`,lesson_type:'realised_profit',summary:`${v.item_summary||v.item_type||'Valuation'} realised profit AUD ${actual.toFixed(2)} versus expected AUD ${expected.toFixed(2)} (${error>=0?'+':''}${error.toFixed(2)} variance).`,source_type:'valuation_history',source_id:String(v.id),evidence:{item_type:v.item_type,item_grade:v.item_grade,status:v.status,expected_profit:expected,actual_profit:actual,bought_price:v.bought_price,sold_price:v.sold_price,forecast_error:error},outcome:error>=0?'outperformed':'underperformed',confidence:0.95,verified:true,active:true,observed_at:v.updated_at||v.created_at,created_by:userId});written++;
  }

  return {written,incident_sample:incidents.length,repair_sample:repairs.length,valuation_sample:vals.length,inventory_sample:inventory.length,sales_sample:sales.length,inventory_open_sample:inventoryOpen,sales_realised_sample:salesRealised};
}

async function summary(){
  const {data,error}=await admin.from('nova_learning_experiences').select('id,domain,lesson_type,summary,source_type,source_id,outcome,confidence,verified,observed_at,created_at').eq('active',true).order('observed_at',{ascending:false,nullsFirst:false}).limit(500);
  if(error) throw error;
  const rows=data||[],byDomain:Record<string,number>={},byType:Record<string,number>={};
  for(const r of rows){byDomain[r.domain]=(byDomain[r.domain]||0)+1;byType[r.lesson_type]=(byType[r.lesson_type]||0)+1;}
  return {count:rows.length,verified_count:rows.filter(r=>r.verified).length,by_domain:byDomain,by_type:byType,recent:rows.slice(0,20)};
}

Deno.serve(async(req:Request)=>{
  const headers=corsHeaders(req),reply=(body:unknown,status=200)=>new Response(JSON.stringify(body),{status,headers});
  if(req.method==='OPTIONS') return new Response('ok',{headers});
  if(req.method!=='POST') return reply({error:'POST required'},405);
  try{
    const auth=await authorise(req);if('error' in auth)return reply({error:auth.error},auth.status);
    let body:any={};try{body=await req.json()}catch{return reply({error:'Invalid JSON request'},400)}
    const action=clean(body?.action,40);
    if(action==='summary') return reply({ok:true,...await summary()});
    if(action==='harvest') return reply({ok:true,...await harvest(auth.user.id)});
    if(action==='feedback'){
      const domain=clean(body?.domain,40),summaryText=clean(body?.summary,1200),allowed=new Set(['guardian','valuation','catalogue','inventory','sales','support','release','pricing','admin']);
      if(!allowed.has(domain)||!summaryText) return reply({error:'Valid domain and feedback summary required'},400);
      const key=`feedback:${auth.user.id}:${crypto.randomUUID()}`;
      await upsertExperience({domain,lesson_key:key,lesson_type:'human_feedback',summary:summaryText,source_type:'admin_feedback',source_id:String(auth.user.id),evidence:{note:'Explicit Admin/Manager feedback; non-authoritative until corroborated by source data.'},outcome:'feedback',confidence:0.6,verified:false,active:true,observed_at:new Date().toISOString(),created_by:auth.user.id});
      return reply({ok:true,lesson_key:key});
    }
    return reply({error:'Unsupported action'},400);
  }catch(error){
    const message=error instanceof Error?error.message:clean(error,500)||'Nova learning backend error';
    console.error(`[nova-learning] ${message}`);
    return reply({error:message},500);
  }
});