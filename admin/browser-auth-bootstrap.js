'use strict';
const SUPABASE_URL='https://ghdhairijqjqivqriigi.supabase.co';
const SUPABASE_KEY='sb_publishable_ch49o8WRnDb8pPzowZH3Tg_XZcIbgvt';
const AUTH_TIMEOUT_MS=12000;
const sb=window.supabase.createClient(SUPABASE_URL,SUPABASE_KEY);
window.sb=sb;
let bootstrapPromise=null;
let bootstrapState='booting';
function setBootstrapState(state,message=''){
  bootstrapState=state;
  document.documentElement.dataset.adminAuthState=state;
  const status=document.getElementById('loginStatus');
  if(status&&message)status.textContent=message;
  try{window.dispatchEvent(new CustomEvent('morley:admin-auth-state',{detail:{state,message}}))}catch(_e){}
}
function withTimeout(promise,label){
  let timer=0;
  const timeout=new Promise((_,reject)=>{timer=setTimeout(()=>{const error=new Error(`${label} timed out. Check your connection and try again.`);error.code='AUTH_TIMEOUT';reject(error)},AUTH_TIMEOUT_MS)});
  return Promise.race([promise,timeout]).finally(()=>clearTimeout(timer));
}
function classifyRecoverable(error){const offline=navigator.onLine===false;return offline||error?.code==='AUTH_TIMEOUT'||/network|fetch|timeout/i.test(String(error?.message||''))?(offline?'offline_recoverable':'error_recoverable'):'error_recoverable'}
function loadSession({retry=false}={}){
  if(retry)bootstrapPromise=null;
  if(bootstrapPromise)return bootstrapPromise;
  setBootstrapState('booting','Restoring your Admin session…');
  bootstrapPromise=(async()=>{
    try{
      const sessionResult=await withTimeout(sb.auth.getSession(),'Session check');
      const session=sessionResult?.data?.session;
      const sessionError=sessionResult?.error;
      if(sessionError){setBootstrapState(classifyRecoverable(sessionError),sessionError.message||'Session check failed.');return false}
      if(!session){setBootstrapState('signed_out','');return false}
      setBootstrapState('authenticated_pending_context','Loading your Admin permissions…');
      const profileResult=await withTimeout(sb.from('profiles').select('id,email,display_name,role,is_enabled').eq('id',session.user.id).single(),'Admin profile check');
      const profile=profileResult?.data,error=profileResult?.error;
      if(error){setBootstrapState(classifyRecoverable(error),error.message||'Admin profile check failed.');return false}
      if(!profile||!profile.is_enabled||!['admin','manager'].includes(profile.role)){
        await withTimeout(sb.auth.signOut(),'Sign out').catch(()=>{});
        setBootstrapState('signed_out','This account is not authorised for Admin Control.');
        return false;
      }
      setBootstrapState('authenticated_ready','Opening Admin Control…');
      location.replace(`workspace.html?auth=${Date.now()}`);
      return true;
    }catch(error){setBootstrapState(classifyRecoverable(error),error?.message||'Session check failed.');return false}
  })();
  return bootstrapPromise;
}
window.loadSession=loadSession;
window.AdminBrowserAuthBootstrap=Object.freeze({loadSession,retry:()=>loadSession({retry:true}),getState:()=>bootstrapState});
loadSession().catch(error=>setBootstrapState(classifyRecoverable(error),error?.message||'Session check failed.'));
