'use strict';
const SUPABASE_URL='https://ghdhairijqjqivqriigi.supabase.co';
const SUPABASE_KEY='sb_publishable_ch49o8WRnDb8pPzowZH3Tg_XZcIbgvt';
const sb=window.supabase.createClient(SUPABASE_URL,SUPABASE_KEY);
window.sb=sb;
async function loadSession(){
  const status=document.getElementById('loginStatus');
  const {data:{session},error:sessionError}=await sb.auth.getSession();
  if(sessionError){if(status)status.textContent=sessionError.message||'Session check failed.';return false;}
  if(!session)return false;
  const {data:profile,error}=await sb.from('profiles').select('id,email,display_name,role,is_enabled').eq('id',session.user.id).single();
  if(error||!profile||!profile.is_enabled||!['admin','manager'].includes(profile.role)){
    await sb.auth.signOut();
    if(status)status.textContent=error?.message||'This account is not authorised for Admin Control.';
    return false;
  }
  location.replace(`workspace.html?auth=${Date.now()}`);
  return true;
}
window.loadSession=loadSession;
loadSession().catch(error=>{const status=document.getElementById('loginStatus');if(status)status.textContent=error?.message||'Session check failed.';});
