(()=>{
'use strict';
const SB='https://ghdhairijqjqivqriigi.supabase.co';
const KEY='sb_publishable_ch49o8WRnDb8pPzowZH3Tg_XZcIbgvt';
const AUTH='morley_web_auth';
const ALLOWED=new Set(['admin','manager']);
const $=(s,r=document)=>r.querySelector(s);
function session(){try{return JSON.parse(localStorage.getItem(AUTH)||'null')}catch{return null}}
function uid(token){try{const p=token.split('.')[1].replace(/-/g,'+').replace(/_/g,'/');return JSON.parse(atob(p+'='.repeat((4-p.length%4)%4))).sub||''}catch{return''}}
async function role(){const s=session(),id=uid(s?.access_token||'');if(!s?.access_token||!id)return null;try{const r=await fetch(`${SB}/rest/v1/profiles?select=role,is_enabled&id=eq.${encodeURIComponent(id)}`,{headers:{apikey:KEY,Authorization:`Bearer ${s.access_token}`},cache:'no-store'});if(!r.ok)return null;const rows=await r.json(),p=rows[0];if(!p?.is_enabled)return null;const value=String(p.role||'user').trim().toLowerCase();return ALLOWED.has(value)?value:null}catch{return null}}
function adminHref(){
  try{
    const base=/^https?:$/i.test(location.protocol)?location.href:location.origin;
    return new URL('admin/',base).href;
  }catch{return '/admin/'}
}
function remove(){document.getElementById('morleyWebAdminGroup')?.remove()}
function insert(roleName){
 const menu=$('#morleyRelevantMore');if(!menu)return false;
 remove();
 const groups=[...menu.querySelectorAll('.morley-menu-group')];
 const dataGroup=groups.find(g=>g.querySelector('h3')?.textContent.trim()==='Data & preferences');
 const g=document.createElement('section');g.id='morleyWebAdminGroup';g.className='morley-menu-group';
 g.innerHTML=`<h3>Administration</h3><div class="morley-menu-card"><button class="morley-menu-row" type="button" data-action="embedded-admin"><span class="morley-menu-icon">◆</span><span class="morley-menu-copy"><b>Admin mode</b><small>Operational controls for ${roleName.charAt(0).toUpperCase()+roleName.slice(1)} accounts. Standard users do not see this area.</small></span><span class="morley-menu-chevron">›</span></button></div>`;
 (dataGroup||menu.querySelector('.morley-menu-signout'))?.before(g);
 g.querySelector('button').addEventListener('click',()=>location.assign(adminHref()));
 return true;
}
let running=false;
async function sync(){if(running)return;running=true;try{const r=await role();if(!r){remove();return}if(!insert(r))setTimeout(sync,250)}finally{running=false}}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',sync,{once:true});else sync();
window.addEventListener('morley-profile-updated',sync);
window.addEventListener('storage',e=>{if(e.key===AUTH)sync()});
window.addEventListener('morley-product-parity-ready',sync);
setInterval(sync,60000);
})();
