(()=>{
const STORE='morley_web_auth',PROFILE='morley_profile_name',SUPABASE_URL='https://ghdhairijqjqivqriigi.supabase.co',API_KEY='sb_publishable_ch49o8WRnDb8pPzowZH3Tg_XZcIbgvt';
const readSession=()=>{try{return JSON.parse(localStorage.getItem(STORE)||'null')}catch{return null}};
const jwtSub=t=>{try{const p=t.split('.')[1].replace(/-/g,'+').replace(/_/g,'/');return JSON.parse(atob(p+'='.repeat((4-p.length%4)%4))).sub||''}catch{return''}};
const firstWord=v=>String(v||'').trim().split(/\s+/)[0]||'';
const badgeName=()=>firstWord(document.querySelector('.morley-web-user')?.textContent);
const sessionName=s=>{const m=s?.user?.user_metadata||{},email=(s?.email||s?.user?.email||'').trim();return firstWord(m.first_name||m.display_name||m.full_name||m.name)||(email?email.split('@')[0]:'')};
function apply(name){name=firstWord(name);if(!name)return false;if(localStorage.getItem(PROFILE)!==name)localStorage.setItem(PROFILE,name);const h=document.querySelector('#morleySmartWorkspace .msw-head h2'),text=`Welcome, ${name}`;if(h&&h.textContent!==text)h.textContent=text;return !!h}
function applyImmediate(){const s=readSession();return apply(badgeName()||localStorage.getItem(PROFILE)||sessionName(s))}
async function fetchProfileOnce(){const s=readSession(),id=jwtSub(s?.access_token||'');if(!s?.access_token||!id)return;try{const r=await fetch(`${SUPABASE_URL}/rest/v1/profiles?select=*&id=eq.${encodeURIComponent(id)}&limit=1`,{headers:{apikey:API_KEY,Authorization:`Bearer ${s.access_token}`},cache:'no-store'});if(!r.ok)return;const p=(await r.json())[0]||{},name=firstWord(p.first_name||p.display_name||p.full_name||p.name);if(name)apply(name)}catch{}}
function boot(){applyImmediate();setTimeout(applyImmediate,450);setTimeout(applyImmediate,1400);setTimeout(fetchProfileOnce,300)}
window.addEventListener('morley-profile-updated',applyImmediate);
window.addEventListener('storage',e=>{if(e.key===STORE||e.key===PROFILE)applyImmediate()});
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot,{once:true});else boot();
})();
