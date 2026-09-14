(()=>{'use strict';
const context=window.__morleyAdminAuthContext;
if(!context?.profile||context.profile.role!=='staff')return;
document.documentElement.dataset.adminAccess='support-only';
const who=document.getElementById('whoami');
const role=document.getElementById('roleText');
if(who)who.textContent=context.profile.display_name||context.profile.email||'Staff';
if(role)role.textContent=`STAFF • SUPPORT ONLY • ${context.profile.email||''}`;
const logout=document.getElementById('logoutBtn');
if(logout)logout.onclick=async()=>{
  if(logout.disabled)return;
  logout.disabled=true;
  try{
    const {error}=await window.sb.auth.signOut();
    if(error)throw error;
    window.__morleyAdminAuthContext=null;
    location.replace('./');
  }catch(error){
    if(role)role.textContent=`Sign out failed: ${error?.message||'Check your connection and try again.'}`;
    logout.disabled=false;
  }
};
window.MorleyAdminSupportOnly=Object.freeze({enabled:true});
})();
