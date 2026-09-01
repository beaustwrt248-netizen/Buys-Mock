(()=>{
const STORE='morley_web_auth',PROFILE='morley_profile_name',SUPABASE_URL='https://ghdhairijqjqivqriigi.supabase.co',API_KEY='sb_publishable_ch49o8WRnDb8pPzowZH3Tg_XZcIbgvt';
const readSession=()=>{try{return JSON.parse(localStorage.getItem(STORE)||'null')}catch{return null}};
const jwtSub=t=>{try{const p=t.split('.')[1].replace(/-/g,'+').replace(/_/g,'/');return JSON.parse(atob(p+'='.repeat((4-p.length%4)%4))).sub||''}catch{return''}};
const sessionName=s=>{const m=s?.user?.user_metadata||{},email=(s?.email||s?.user?.email||'').trim();return String(m.display_name||m.full_name||m.name||'').trim()||(email?email.split('@')[0]:'')};
async function fetchName(s){const id=jwtSub(s?.access_token||'');if(!id)return sessionName(s);try{const r=await fetch(`${SUPABASE_URL}/rest/v1/profiles?select=*&id=eq.${encodeURIComponent(id)}&limit=1`,{headers:{apikey:API_KEY,Authorization:`Bearer ${s.access_token}`},cache:'no-store'});if(r.ok){const p=(await r.json())[0]||{},joined=[p.first_name,p.last_name].filter(Boolean).join(' ').trim();return String(p.display_name||joined||p.full_name||p.name||'').trim()||sessionName(s)}}catch{}return sessionName(s)}
function apply(name){name=String(name||'').trim();if(!name)return false;localStorage.setItem(PROFILE,name);const h=document.querySelector('#morleySmartWorkspace .msw-head h2');if(h)h.textContent=`Welcome, ${name.split(/\s+/)[0]}`;return !!h}
async function sync(){const s=readSession();if(!s?.access_token)return false;const fallback=sessionName(s);if(fallback)apply(fallback);const resolved=await fetchName(s);if(resolved)apply(resolved);window.dispatchEvent(new Event('morley-profile-updated'));return !!(resolved||fallback)}
let tries=0;const timer=setInterval(()=>{sync();if(++tries>120)clearInterval(timer)},250);
const observer=new MutationObserver(()=>{const s=readSession();if(!s?.access_token)return;const saved=(localStorage.getItem(PROFILE)||'').trim()||sessionName(s);if(saved)apply(saved)});
if(document.body)observer.observe(document.body,{childList:true,subtree:true});
window.addEventListener('storage',e=>{if(e.key===STORE||e.key===PROFILE)sync()});
window.addEventListener('morley-profile-updated',()=>{const s=readSession();const saved=(localStorage.getItem(PROFILE)||'').trim()||sessionName(s);if(saved)apply(saved)});
sync();
})();
