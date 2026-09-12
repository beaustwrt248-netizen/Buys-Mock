(()=>{
'use strict';
const PRIVILEGED_ROLES=new Set(['admin','manager']);
const MUTATING_ACTIONS=new Set(['disable','enable','force_signout','delete','set_role','set_display_name']);
function normalizeRole(role){return String(role||'').trim().toLowerCase()}
function canManageUser(actor,target,action,nextRole){
 const actorRole=normalizeRole(actor?.role),targetRole=normalizeRole(target?.role),next=normalizeRole(nextRole);
 if(!actor?.id||!target?.id||!MUTATING_ACTIONS.has(action))return {allowed:false,reason:'Invalid user-management request.'};
 if(actorRole!=='admin')return {allowed:false,reason:'Only admins can change user accounts.'};
 if(actor.id===target.id&&['disable','force_signout','delete','set_role'].includes(action))return {allowed:false,reason:'You cannot perform that action on your own admin account.'};
 if(action==='set_role'&&!['staff','manager','admin'].includes(next))return {allowed:false,reason:'Select a valid staff role.'};
 if(action==='delete'&&PRIVILEGED_ROLES.has(targetRole))return {allowed:false,reason:'Privileged accounts must be demoted before deletion.'};
 return {allowed:true,reason:''};
}
function auditSummary(action,target,extra={}){
 const details={action,target_user_id:target?.id||null,target_role:normalizeRole(target?.role)};
 if(action==='set_role')details.new_role=normalizeRole(extra.role);
 if(action==='set_display_name')details.display_name=String(extra.display_name||'').trim();
 return details;
}
window.AdminUserManagementPolicy=Object.freeze({canManageUser,auditSummary});

// Native Admin authentication must be installed into the exact Supabase client
// used by app.js. In nativeAuth mode, hold the first getSession() call until
// Android supplies the already-authorised access/refresh tokens. This prevents
// the workspace from observing a transient signed-out state and rendering the
// browser login before the native session handoff has completed.
const nativeAuthMode=new URLSearchParams(location.search).get('nativeAuth')==='1';
if(nativeAuthMode&&window.supabase&&typeof window.supabase.createClient==='function'){
 const originalCreateClient=window.supabase.createClient.bind(window.supabase);
 let nativeClient=null;
 let originalGetSession=null;
 let resolveNativeSessionGate;
 const nativeSessionGate=new Promise(resolve=>{resolveNativeSessionGate=resolve;});
 window.__morleyNativeSessionState='waiting';
 window.__morleyNativeSessionError='';

 window.supabase.createClient=(...args)=>{
  const client=originalCreateClient(...args);
  if(!nativeClient&&client?.auth){
   nativeClient=client;
   originalGetSession=client.auth.getSession.bind(client.auth);
   client.auth.getSession=async(...getSessionArgs)=>{
    if(window.__morleyNativeSessionState==='ready')return originalGetSession(...getSessionArgs);
    if(window.__morleyNativeSessionState==='error')return {data:{session:null},error:new Error(window.__morleyNativeSessionError||'Native Admin session failed.')};
    return nativeSessionGate;
   };
  }
  return client;
 };

 window.installNativeAdminSession=async(accessToken,refreshToken)=>{
  if(window.__morleyNativeSessionState==='ready')return true;
  if(!accessToken||!refreshToken||!nativeClient||!originalGetSession){
   window.__morleyNativeSessionState='error';
   window.__morleyNativeSessionError='Native Admin session handoff was incomplete.';
   resolveNativeSessionGate({data:{session:null},error:new Error(window.__morleyNativeSessionError)});
   return false;
  }
  try{
   const {data,error}=await nativeClient.auth.setSession({access_token:String(accessToken),refresh_token:String(refreshToken)});
   if(error)throw error;
   if(!data?.session)throw new Error('Native Admin session could not be established.');
   const verified=await originalGetSession();
   if(verified?.error)throw verified.error;
   if(!verified?.data?.session)throw new Error('Native Admin session verification failed.');
   window.__morleyNativeSessionState='ready';
   resolveNativeSessionGate(verified);
   return true;
  }catch(error){
   window.__morleyNativeSessionState='error';
   window.__morleyNativeSessionError=error?.message||'Native Admin session failed.';
   resolveNativeSessionGate({data:{session:null},error:error instanceof Error?error:new Error(window.__morleyNativeSessionError)});
   return false;
  }
 };
}
})();
