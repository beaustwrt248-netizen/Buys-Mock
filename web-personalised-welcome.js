(()=>{
const STORE='morley_web_auth',PROFILE='morley_profile_name',SUPABASE_URL='https://ghdhairijqjqivqriigi.supabase.co',API_KEY='sb_publishable_ch49o8WRnDb8pPzowZH3Tg_XZcIbgvt';
const readSession=()=>{try{return JSON.parse(localStorage.getItem(STORE)||'null')}catch{return null}};
const jwtSub=t=>{try{const p=t.split('.')[1].replace(/-/g,'+').replace(/_/g,'/');return JSON.parse(atob(p+'='.repeat((4-p.length%4)%4))).sub||''}catch{return''}};
const firstWord=v=>String(v||'').trim().split(/\s+/)[0]||'';
const sessionName=s=>{const m=s?.user?.user_metadata||{},email=(s?.email||s?.user?.email||'').trim();return firstWord(m.first_name||m.display_name||m.full_name||m.name)||(email?email.split('@')[0]:'')};
async function fetchName(s){const id=jwtSub(s?.access_token||'');if(!id)return sessionName(s);try{const r=await fetch(`${SUPABASE_URL}/rest/v1/profiles?select=*&id=eq.${encodeURIComponent(id)}&limit=1`,{headers:{apikey:API_KEY,Authorization:`Bearer ${s.access_token}`},cache:'no-store'});if(r.ok){const p=(await r.json())[0]||{};return firstWord(p.first_name||p.display_name||p.full_name||p.name)||sessionName(s)}}catch{}return sessionName(s)}
function apply(name){name=firstWord(name);if(!name)return false;localStorage.setItem(PROFILE,name);const h=document.querySelector('#morleySmartWorkspace .msw-head h2');if(h)h.textContent=`Welcome, ${name}`;return !!h}
async function sync(){const s=readSession();if(!s?.access_token)return false;const badge=document.querySelector('.morley-web-user')?.textContent?.trim();if(badge)apply(badge);const resolved=await fetchName(s);if(resolved)apply(resolved);return !!(resolved||badge)}
let tries=0;const timer=setInterval(()=>{sync();if(++tries>120)clearInterval(timer)},250);
const observer=new MutationObserver(()=>{const badge=document.querySelector('.morley-web-user')?.textContent?.trim();if(badge)apply(badge);else{const saved=(localStorage.getItem(PROFILE)||'').trim();if(saved)apply(saved)}});
if(document.body)observer.observe(document.body,{childList:true,subtree:true});
window.addEventListener('storage',e=>{if(e.key===STORE||e.key===PROFILE)sync()});
window.addEventListener('morley-profile-updated',()=>{const badge=document.querySelector('.morley-web-user')?.textContent?.trim();if(badge)apply(badge)});
sync();
})();
