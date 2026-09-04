import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "jsr:@supabase/supabase-js@2";

const SUPABASE_URL = Deno.env.get('SUPABASE_URL') || '';
const SERVICE_ROLE = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') || '';
const ADMIN_ORIGINS = new Set(['https://buyshub.me','https://www.buyshub.me','https://beaustwrt248-netizen.github.io']);
const admin = createClient(SUPABASE_URL, SERVICE_ROLE, { auth: { persistSession: false, autoRefreshToken: false } });

function corsHeaders(req: Request) {
  const origin = req.headers.get('Origin') || '';
  const allowedOrigin = ADMIN_ORIGINS.has(origin) ? origin : 'https://buyshub.me';
  return {
    'Content-Type':'application/json',
    'Access-Control-Allow-Origin':allowedOrigin,
    'Access-Control-Allow-Headers':'authorization, x-client-info, apikey, content-type',
    'Access-Control-Allow-Methods':'POST, OPTIONS',
    'Vary':'Origin','Cache-Control':'no-store','X-Content-Type-Options':'nosniff'
  };
}

const clean=(v:unknown,max=240)=>String(v??'').trim().replace(/\s+/g,' ').slice(0,max);
const categoryMap:Record<string,string>={mobile_phone:'phone',tablet:'tablet',laptop:'laptop',desktop:'desktop',console:'console'};
const normalise=(v:unknown)=>clean(v,240).toLowerCase().replace(/[^a-z0-9]+/g,' ').trim();
const moneyValue=(v:unknown)=>v===null||v===''?null:Number(v);
function errorText(error:unknown){
  if(error instanceof Error) return error.message;
  if(error && typeof error === 'object'){
    const e=error as Record<string,unknown>;
    return clean(e.message || e.details || e.hint || e.code || JSON.stringify(e),500) || 'Pricing backend error';
  }
  return clean(error,500)||'Pricing backend error';
}
function fail(stage:string,error:unknown){
  const message=errorText(error);
  console.error(`[admin-pricing-control] ${stage}: ${message}`);
  return new Error(`${stage}: ${message}`);
}

async function getActiveDevices(){
  const {data,error}=await admin.from('device_catalog')
    .select('id,category,brand,model_name,model_number,storage_options,active')
    .eq('active',true)
    .in('category',['mobile_phone','tablet','laptop','desktop','console'])
    .order('brand').order('model_name').limit(5000);
  if(error) throw fail('device catalogue query failed',error);
  return data||[];
}

function scoreDevice(query:string,d:any){
  const q=normalise(query), hay=normalise([d.brand,d.model_name,d.model_number].filter(Boolean).join(' '));
  if(!q) return 0;
  let score=0;
  const modelNumber=normalise(d.model_number);
  if(modelNumber&&q.includes(modelNumber)) score+=120;
  if(q.includes(normalise(`${d.brand} ${d.model_name}`))) score+=100;
  else if(q.includes(normalise(d.model_name))) score+=80;
  const toks=q.split(' ').filter((x:string)=>x.length>1);
  score+=toks.filter((t:string)=>hay.includes(t)).length*5;
  return score;
}

function parseAssistantLine(line:string,devices:any[]){
  const priceMatch=line.match(/(?:\$|aud\s*)(\d+(?:\.\d{1,2})?)/i)||line.match(/(?:\bto\b|\bat\b|=)\s*\$?\s*(\d+(?:\.\d{1,2})?)/i);
  const price=priceMatch?Number(priceMatch[1]):null;
  if(price!==null&&(!Number.isFinite(price)||price<0)) return {input:line,error:'Invalid price.'};
  const storageMatches=[...line.matchAll(/\b(\d+(?:\.\d+)?)\s*(TB|GB)\b/ig)].map(m=>`${m[1]}${m[2].toUpperCase()}`);
  const ranked=devices.map(d=>({d,score:scoreDevice(line,d)})).filter(x=>x.score>0).sort((a,b)=>b.score-a.score).slice(0,5);
  if(!ranked.length) return {input:line,price_aud:price,error:'No matching live catalogue device found.'};
  const top=ranked[0];
  const storageOptions=(top.d.storage_options||[]).map((x:string)=>String(x));
  const storage=storageMatches.find(s=>storageOptions.some((x:string)=>normalise(x)===normalise(s)))||(storageOptions.length===1?storageOptions[0]:'');
  return {input:line,price_aud:price,storage,confidence:Math.min(1,top.score/120),device:{device_catalog_id:top.d.id,category:categoryMap[top.d.category]||top.d.category,brand:top.d.brand,model:top.d.model_name,model_number:top.d.model_number,storage_options:storageOptions},alternatives:ranked.slice(1,4).map(x=>({device_catalog_id:x.d.id,brand:x.d.brand,model:x.d.model_name,model_number:x.d.model_number,score:x.score}))};
}

Deno.serve(async(req:Request)=>{
  const headers=corsHeaders(req);
  const reply=(body:unknown,status=200)=>new Response(JSON.stringify(body),{status,headers});
  if(req.method==='OPTIONS') return new Response('ok',{headers});
  if(req.method!=='POST') return reply({error:'POST required'},405);
  try {
    if(!SUPABASE_URL||!SERVICE_ROLE) throw new Error('Pricing backend configuration unavailable');
    const token=(req.headers.get('Authorization')||'').replace(/^Bearer\s+/i,'');
    if(!token) return reply({error:'Authentication required'},401);
    const {data:{user},error:userError}=await admin.auth.getUser(token);
    if(userError||!user) return reply({error:'Invalid session'},401);
    const {data:caller,error:callerError}=await admin.from('profiles').select('id,role,is_enabled').eq('id',user.id).maybeSingle();
    if(callerError) throw fail('admin profile query failed',callerError);
    if(!caller?.is_enabled||!['admin','manager'].includes(caller.role)) return reply({error:'Admin or Manager access required'},403);

    let body:any={};
    try{body=await req.json()}catch{return reply({error:'Invalid JSON request'},400)}
    const action=clean(body?.action,40);

    if(action==='list'){
      const devices=await getActiveDevices();
      const {data:prices,error:priceError}=await admin.from('device_buy_prices').select('id,device_catalog_id,storage,condition_grade,price_aud,authoritative,source,notes,is_active,version').eq('is_active',true).limit(10000);
      if(priceError) throw fail('pricing query failed',priceError);
      const priceMap=new Map((prices||[]).map((p:any)=>[`${p.device_catalog_id}|${normalise(p.storage)}|${p.condition_grade}`,p]));
      const items:any[]=[];
      for(const d of devices){
        const storages=Array.isArray(d.storage_options)&&d.storage_options.length?d.storage_options:[''];
        for(const storageValue of storages){
          const storage=String(storageValue??'');
          const p:any=priceMap.get(`${d.id}|${normalise(storage)}|base`);
          items.push({id:p?.id||`device-${d.id}-${storage||'base'}`,price_id:p?.id||null,device_catalog_id:d.id,category:categoryMap[d.category]||d.category,brand:d.brand,model:d.model_name,model_number:d.model_number,storage,price_aud:p?.price_aud??null,authoritative:p?.authoritative===true,source:p?.source||'',notes:p?.notes||'',is_active:p?.is_active!==false,version:p?.version??0});
        }
      }
      return reply({ok:true,items,count:items.length});
    }

    if(action==='history'){
      const priceId=clean(body?.price_id||body?.catalogue_item_id,80);
      if(!priceId||priceId.startsWith('device-')) return reply({ok:true,history:[]});
      const {data,error}=await admin.from('device_buy_price_history').select('*').eq('device_buy_price_id',priceId).order('changed_at',{ascending:false}).limit(200);
      if(error) throw fail('price history query failed',error);
      return reply({ok:true,history:data||[]});
    }

    if(action==='assistant_preview'){
      const text=String(body?.text||'').trim().slice(0,12000);
      if(!text) return reply({error:'Device or pricing instruction required'},400);
      const devices=await getActiveDevices();
      const lines=text.split(/\n|;/).map(x=>x.trim()).filter(Boolean).slice(0,30);
      return reply({ok:true,proposals:lines.map(line=>parseAssistantLine(line,devices))});
    }

    if(action==='update'){
      const deviceId=Number(body?.device_catalog_id);
      if(!Number.isInteger(deviceId)||deviceId<=0) return reply({error:'device_catalog_id required'},400);
      const storage=clean(body?.storage,80),conditionGrade=clean(body?.condition_grade||'base',40)||'base';
      const {data:device,error:deviceError}=await admin.from('device_catalog').select('id,brand,model_name,model_number,storage_options,active').eq('id',deviceId).maybeSingle();
      if(deviceError) throw fail('device lookup failed',deviceError);
      if(!device?.active) return reply({error:'Active catalogue device not found'},404);
      const allowedStorage=(device.storage_options||[]).map((x:string)=>String(x));
      const canonicalStorage=allowedStorage.find((x:string)=>normalise(x)===normalise(storage))??storage;
      if(allowedStorage.length&&(!storage||!allowedStorage.some((x:string)=>normalise(x)===normalise(storage)))) return reply({error:'Storage must match a catalogue storage option'},400);
      const price=moneyValue(body?.price_aud);
      const authoritative=body?.authoritative===true;
      if(price!==null&&(!Number.isFinite(price)||price<0)) return reply({error:'Invalid price'},400);
      if(authoritative&&price===null) return reply({error:'Authoritative rows require a price'},400);
      const source=clean(body?.source||'Morley pricing',120),notes=clean(body?.notes||'',500),isActive=body?.is_active!==false;
      const {data:existing,error:existingError}=await admin.from('device_buy_prices').select('*').eq('device_catalog_id',deviceId).eq('storage',canonicalStorage).eq('condition_grade',conditionGrade).maybeSingle();
      if(existingError) throw fail('existing price lookup failed',existingError);
      let saved:any;
      if(existing){
        const {data,error}=await admin.from('device_buy_prices').update({price_aud:price,authoritative,source,notes,is_active:isActive,updated_by:user.id,updated_at:new Date().toISOString(),version:(existing.version||1)+1}).eq('id',existing.id).select('*').single();
        if(error) throw fail('price update failed',error); saved=data;
      }else{
        const {data,error}=await admin.from('device_buy_prices').insert({device_catalog_id:deviceId,storage:canonicalStorage,condition_grade:conditionGrade,price_aud:price,authoritative,source,notes,is_active:isActive,updated_by:user.id}).select('*').single();
        if(error) throw fail('price insert failed',error); saved=data;
      }
      const {error:historyError}=await admin.from('device_buy_price_history').insert({device_buy_price_id:saved.id,device_catalog_id:deviceId,storage:canonicalStorage,condition_grade:conditionGrade,old_price_aud:existing?.price_aud??null,new_price_aud:saved.price_aud,old_authoritative:existing?.authoritative===true,new_authoritative:saved.authoritative===true,source,notes,changed_by:user.id});
      if(historyError) throw fail('price history insert failed',historyError);
      const {error:auditError}=await admin.from('admin_audit_log').insert({actor_user_id:user.id,action:existing?'device_pricing_update':'device_pricing_create',target_type:'device_catalog',target_id:String(deviceId),details:{brand:device.brand,model:device.model_name,model_number:device.model_number,storage:canonicalStorage,old_price_aud:existing?.price_aud??null,new_price_aud:saved.price_aud,authoritative:saved.authoritative,source}});
      if(auditError) throw fail('admin audit insert failed',auditError);
      return reply({ok:true,item:{id:saved.id,price_id:saved.id,device_catalog_id:deviceId,brand:device.brand,model:device.model_name,model_number:device.model_number,storage:canonicalStorage,price_aud:saved.price_aud,authoritative:saved.authoritative,source:saved.source,notes:saved.notes,is_active:saved.is_active,version:saved.version}});
    }

    return reply({error:'Unsupported action'},400);
  } catch(error) {
    const message=errorText(error);
    console.error(`[admin-pricing-control] request failed: ${message}`);
    return reply({error:message},500);
  }
});
