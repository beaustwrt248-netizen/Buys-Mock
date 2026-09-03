import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "jsr:@supabase/supabase-js@2";

const SUPABASE_URL = Deno.env.get('SUPABASE_URL')!;
const SERVICE_ROLE = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!;
const ADMIN_ORIGINS = new Set(['https://buyshub.me','https://www.buyshub.me','https://beaustwrt248-netizen.github.io']);
const admin = createClient(SUPABASE_URL, SERVICE_ROLE, { auth: { persistSession: false } });

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

const clean=(v:unknown,max=160)=>String(v??'').trim().replace(/\s+/g,' ').slice(0,max);
const validCategory=(v:string)=>['phone','laptop','console','general'].includes(v);

Deno.serve(async(req:Request)=>{
  const headers=corsHeaders(req);
  const reply=(body:unknown,status=200)=>new Response(JSON.stringify(body),{status,headers});
  if(req.method==='OPTIONS') return new Response('ok',{headers});
  if(req.method!=='POST') return reply({error:'POST required'},405);
  try {
    const token=(req.headers.get('Authorization')||'').replace(/^Bearer\s+/i,'');
    if(!token) return reply({error:'Authentication required'},401);
    const {data:{user},error:userError}=await admin.auth.getUser(token);
    if(userError||!user) return reply({error:'Invalid session'},401);
    const {data:caller}=await admin.from('profiles').select('id,role,is_enabled').eq('id',user.id).single();
    if(!caller?.is_enabled||!['admin','manager'].includes(caller.role)) return reply({error:'Admin or Manager access required'},403);

    const body=await req.json();
    const action=clean(body?.action,40);

    if(action==='list'){
      const category=clean(body?.category,30);
      let query=admin.from('morley_catalogue_items').select('*').eq('is_active',true).order('brand').order('model').order('storage');
      if(category){if(!validCategory(category)) return reply({error:'Invalid category'},400);query=query.eq('category',category)}
      const {data,error}=await query;if(error) throw error;
      return reply({ok:true,items:data||[]});
    }

    if(action==='history'){
      const itemId=clean(body?.catalogue_item_id,80);if(!itemId) return reply({error:'catalogue_item_id required'},400);
      const {data,error}=await admin.from('morley_price_history').select('*').eq('catalogue_item_id',itemId).order('changed_at',{ascending:false}).limit(200);if(error) throw error;
      return reply({ok:true,history:data||[]});
    }

    const category=clean(body?.category,30),brand=clean(body?.brand),model=clean(body?.model),modelNumber=clean(body?.model_number),storage=clean(body?.storage,80),source=clean(body?.source||'Morley catalogue',120);
    if(action==='create'){
      if(!validCategory(category)||!model) return reply({error:'Valid category and model required'},400);
      const price=body?.price_aud===null||body?.price_aud===''?null:Number(body?.price_aud);
      const authoritative=body?.authoritative===true;
      if(price!==null&&(!Number.isFinite(price)||price<0)) return reply({error:'Invalid price'},400);
      if(authoritative&&price===null) return reply({error:'Authoritative rows require a price'},400);
      const {data,error}=await admin.from('morley_catalogue_items').insert({category,brand,model,model_number:modelNumber,storage,price_aud:price,authoritative,source,updated_by:user.id}).select('*').single();if(error) throw error;
      await admin.from('admin_audit_log').insert({actor_user_id:user.id,action:'pricing_create',target_type:'catalogue_item',target_id:data.id,details:{category,brand,model,storage,price_aud:price,authoritative}});
      return reply({ok:true,item:data});
    }

    if(action==='update'){
      const id=clean(body?.catalogue_item_id,80);if(!id) return reply({error:'catalogue_item_id required'},400);
      const {data:existing}=await admin.from('morley_catalogue_items').select('*').eq('id',id).single();if(!existing) return reply({error:'Catalogue item not found'},404);
      const patch:Record<string,unknown>={updated_by:user.id};
      if(body?.price_aud!==undefined){const price=body.price_aud===null||body.price_aud===''?null:Number(body.price_aud);if(price!==null&&(!Number.isFinite(price)||price<0)) return reply({error:'Invalid price'},400);patch.price_aud=price;}
      if(body?.authoritative!==undefined) patch.authoritative=body.authoritative===true;
      if(body?.source!==undefined) patch.source=source;
      if(body?.is_active!==undefined) patch.is_active=body.is_active===true;
      const nextPrice=patch.price_aud!==undefined?patch.price_aud:existing.price_aud;
      const nextAuth=patch.authoritative!==undefined?patch.authoritative:existing.authoritative;
      if(nextAuth===true&&nextPrice===null) return reply({error:'Authoritative rows require a price'},400);
      const {data,error}=await admin.from('morley_catalogue_items').update(patch).eq('id',id).select('*').single();if(error) throw error;
      await admin.from('admin_audit_log').insert({actor_user_id:user.id,action:'pricing_update',target_type:'catalogue_item',target_id:id,details:{old_price_aud:existing.price_aud,new_price_aud:data.price_aud,old_authoritative:existing.authoritative,new_authoritative:data.authoritative}});
      return reply({ok:true,item:data});
    }

    return reply({error:'Unsupported action'},400);
  } catch(error) {
    return reply({error:error instanceof Error?error.message:String(error)},500);
  }
});
