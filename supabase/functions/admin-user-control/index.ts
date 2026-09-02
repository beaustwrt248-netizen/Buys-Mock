import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "jsr:@supabase/supabase-js@2";

const SUPABASE_URL = Deno.env.get('SUPABASE_URL')!;
const SERVICE_ROLE = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!;
const ADMIN_ORIGINS = new Set([
  'https://buyshub.me',
  'https://www.buyshub.me',
  'https://beaustwrt248-netizen.github.io',
]);
const admin = createClient(SUPABASE_URL, SERVICE_ROLE, { auth: { persistSession: false } });

function corsHeaders(req: Request) {
  const origin = req.headers.get('Origin') || '';
  const allowedOrigin = ADMIN_ORIGINS.has(origin) ? origin : 'https://buyshub.me';
  return {
    'Content-Type': 'application/json',
    'Access-Control-Allow-Origin': allowedOrigin,
    'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
    'Access-Control-Allow-Methods': 'POST, OPTIONS',
    'Vary': 'Origin',
    'Cache-Control': 'no-store',
    'X-Content-Type-Options': 'nosniff',
  };
}

function cleanName(value: unknown) { return String(value ?? '').trim().replace(/\s+/g, ' '); }
function validEmail(value: string) { return value.length >= 3 && value.length <= 254 && value.includes('@') && value.split('@')[1]?.includes('.'); }
function makeTemporaryPassword() {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789';
  const bytes = new Uint8Array(12);
  crypto.getRandomValues(bytes);
  const random = Array.from(bytes, b => chars[b % chars.length]).join('');
  return `M!${random}7a`;
}

Deno.serve(async(req:Request)=>{
  const headers = corsHeaders(req);
  const reply=(body:unknown,status=200)=>new Response(JSON.stringify(body),{status,headers});
  if(req.method==='OPTIONS') return new Response('ok',{headers});
  if(req.method!=='POST') return reply({error:'POST required'},405);
  try{
    const token=(req.headers.get('Authorization')||'').replace(/^Bearer\s+/i,'');
    if(!token) return reply({error:'Authentication required'},401);
    const {data:{user},error:userError}=await admin.auth.getUser(token);
    if(userError||!user) return reply({error:'Invalid session'},401);
    const {data:caller}=await admin.from('profiles').select('id,role,is_enabled').eq('id',user.id).single();
    if(!caller?.is_enabled) return reply({error:'Administrator access required'},403);
    const body=await req.json();
    const action=String(body?.action||'');

    if(action==='create_user'){
      if(!['admin','manager'].includes(caller.role)) return reply({error:'Admin or Manager access required'},403);
      const email=String(body?.email||'').trim().toLowerCase();
      const displayName=cleanName(body?.display_name);
      const role=String(body?.role||'').trim().toLowerCase();
      const temporaryPassword=String(body?.temporary_password||'');
      const skipVerification=body?.skip_email_verification===true;
      const parts=displayName.split(' ').filter(Boolean);
      if(!validEmail(email)) return reply({error:'Enter a valid email address'},400);
      if(displayName.length<3||displayName.length>100||parts.length<2) return reply({error:'Enter a valid first and last name'},400);
      if(!['staff','manager'].includes(role)) return reply({error:'Invalid role'},400);
      if(caller.role==='manager'&&role!=='staff') return reply({error:'Managers may create Staff accounts only'},403);
      if(temporaryPassword.length<10||temporaryPassword.length>256) return reply({error:'Temporary password must be between 10 and 256 characters'},400);
      if(!skipVerification) return reply({error:'Use the secure invite flow when email verification is required'},400);
      const firstName=parts[0],lastName=parts.slice(1).join(' ');
      const {data:created,error:createError}=await admin.auth.admin.createUser({email,password:temporaryPassword,email_confirm:true,user_metadata:{first_name:firstName,last_name:lastName,full_name:displayName,must_change_password:true,temporary_password_account:true,provisioned_by_admin:true}});
      if(createError){const msg=createError.message?.toLowerCase().includes('already')?'An account already exists for this email.':createError.message;return reply({error:msg},400);}
      const targetUser=created.user.id;
      const {error:profileError}=await admin.from('profiles').upsert({id:targetUser,email,display_name:displayName,role,is_enabled:true,updated_at:new Date().toISOString()},{onConflict:'id'});
      if(profileError){await admin.auth.admin.deleteUser(targetUser);throw profileError;}
      await admin.from('admin_audit_log').insert({actor_user_id:user.id,action:'user_create_temporary_password',target_type:'user',target_id:targetUser,details:{email,role,skip_email_verification:true,must_change_password:true}});
      return reply({ok:true,action,target_user_id:targetUser,requires_password_change:true});
    }

    if(caller.role!=='admin') return reply({error:'Administrator access required'},403);
    const targetUser=String(body?.target_user_id||'');
    if(!targetUser) return reply({error:'target_user_id required'},400);
    if(targetUser===user.id && ['disable','force_signout','delete','set_role','reset_password'].includes(action)) return reply({error:'You cannot perform this action on your own active admin account'},400);
    const {data:targetProfile}=await admin.from('profiles').select('id,email,display_name,role,is_enabled').eq('id',targetUser).single();
    if(!targetProfile) return reply({error:'User profile not found'},404);
    let details: Record<string, unknown> = {email:targetProfile.email, role:targetProfile.role};
    if(action==='force_signout'){const {error}=await admin.auth.admin.signOut(targetUser,'global');if(error) throw error;}
    else if(action==='disable'){const {error}=await admin.from('profiles').update({is_enabled:false,updated_at:new Date().toISOString()}).eq('id',targetUser);if(error) throw error;const {error:signoutError}=await admin.auth.admin.signOut(targetUser,'global');if(signoutError) throw signoutError;}
    else if(action==='enable'){const {error}=await admin.from('profiles').update({is_enabled:true,updated_at:new Date().toISOString()}).eq('id',targetUser);if(error) throw error;}
    else if(action==='set_role'){const role=String(body?.role||'').toLowerCase();if(!['staff','manager','admin'].includes(role)) return reply({error:'Invalid role'},400);const {error}=await admin.from('profiles').update({role,updated_at:new Date().toISOString()}).eq('id',targetUser);if(error) throw error;details={...details,new_role:role};}
    else if(action==='set_display_name'){const displayName=cleanName(body?.display_name);const parts=displayName.split(' ').filter(Boolean);if(displayName.length<3||displayName.length>100||parts.length<2) return reply({error:'Enter a valid first and last name'},400);const firstName=parts[0],lastName=parts.slice(1).join(' ');const {error}=await admin.from('profiles').update({display_name:displayName,updated_at:new Date().toISOString()}).eq('id',targetUser);if(error) throw error;const {error:authError}=await admin.auth.admin.updateUserById(targetUser,{user_metadata:{first_name:firstName,last_name:lastName,full_name:displayName}});if(authError) throw authError;details={...details,old_display_name:targetProfile.display_name,new_display_name:displayName};}
    else if(action==='reset_password'){
      const temporaryPassword=makeTemporaryPassword();
      const {data:authUser,error:getError}=await admin.auth.admin.getUserById(targetUser);
      if(getError||!authUser?.user) throw getError || new Error('Auth user not found');
      const metadata={...(authUser.user.user_metadata||{}),must_change_password:true,temporary_password_account:true,password_reset_by_admin:true};
      const {error:updateError}=await admin.auth.admin.updateUserById(targetUser,{password:temporaryPassword,user_metadata:metadata});
      if(updateError) throw updateError;
      const {error:signoutError}=await admin.auth.admin.signOut(targetUser,'global');
      if(signoutError) throw signoutError;
      await admin.from('admin_audit_log').insert({actor_user_id:user.id,action:'user_reset_password',target_type:'user',target_id:targetUser,details:{email:targetProfile.email,role:targetProfile.role,must_change_password:true,temporary_password_issued:true}});
      return reply({ok:true,action,target_user_id:targetUser,temporary_password:temporaryPassword,requires_password_change:true});
    }
    else if(action==='delete'){const {error}=await admin.auth.admin.deleteUser(targetUser,false);if(error) throw error;}
    else return reply({error:'Unsupported action'},400);
    await admin.from('admin_audit_log').insert({actor_user_id:user.id,action:`user_${action}`,target_type:'user',target_id:targetUser,details});
    return reply({ok:true,action,target_user_id:targetUser});
  }catch(error){return reply({error:error instanceof Error?error.message:String(error)},500)}
});
