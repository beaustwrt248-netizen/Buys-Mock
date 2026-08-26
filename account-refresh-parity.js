(()=>{
  const SB='https://ghdhairijqjqivqriigi.supabase.co';
  const KEY='sb_publishable_ch49o8WRnDb8pPzowZH3Tg_XZcIbgvt';
  const AUTH='morley_web_auth';
  const $=(s,r=document)=>r.querySelector(s);

  function session(){try{return JSON.parse(localStorage.getItem(AUTH)||'null')}catch{return null}}
  function uid(token){try{let p=token.split('.')[1].replace(/-/g,'+').replace(/_/g,'/');return JSON.parse(atob(p+'='.repeat((4-p.length%4)%4))).sub||''}catch{return''}}
  function button(){return '<button id="morleyRefreshAccount" type="button" style="width:100%;margin-top:10px;padding:12px;border-radius:12px;border:1px solid rgba(47,124,255,.35);background:#0a1b33;color:#fff;font-weight:900;cursor:pointer">Refresh Account</button>'}
  function message(d,text,ok=true){let m=$('.morley-account-refresh-msg',d);if(!m){m=document.createElement('div');m.className='morley-account-refresh-msg';m.setAttribute('role','status');m.setAttribute('aria-live','polite');m.style.cssText='margin-top:10px;padding:10px 12px;border-radius:10px;font-size:12px';$('#morleyRefreshAccount',d)?.insertAdjacentElement('afterend',m)}m.textContent=text;m.style.background=ok?'rgba(37,217,145,.1)':'rgba(255,90,110,.1)';m.style.color=ok?'#63efb5':'#ff9aa8'}

  async function refresh(d){
    const b=$('#morleyRefreshAccount',d);if(!b)return;
    const s=session(),id=uid(s?.access_token||'');
    if(!s?.access_token||!id){message(d,'Your session is not available. Sign in again.',false);return}
    const old=b.textContent;b.disabled=true;b.textContent='Refreshing…';
    try{
      const r=await fetch(`${SB}/rest/v1/profiles?select=id,email,display_name,is_enabled&id=eq.${encodeURIComponent(id)}`,{headers:{apikey:KEY,Authorization:`Bearer ${s.access_token}`},cache:'no-store'});
      if(!r.ok)throw new Error(`Account service returned HTTP ${r.status}`);
      const p=(await r.json())[0];
      if(!p)throw new Error('Profile record was not found.');
      const email=$('#mmEmail',d),name=$('#mmName',d);
      if(email)email.value=p.email||s.email||'';
      if(name)name.value=p.display_name||'';
      if(p.display_name)localStorage.setItem('morley_profile_name',p.display_name);
      window.dispatchEvent(new Event('morley-profile-updated'));
      message(d,'Account profile refreshed.');
    }catch(e){message(d,e.message||String(e),false)}finally{b.disabled=false;b.textContent=old}
  }

  function enhance(){
    const d=$('#morleyMenuDialog.open');if(!d||$('h2',d)?.textContent?.trim()!=='Account & Profile')return;
    const save=$('#mmSave',d);if(!save||$('#morleyRefreshAccount',d))return;
    save.insertAdjacentHTML('afterend',button());
    $('#morleyRefreshAccount',d).onclick=()=>refresh(d);
  }

  function init(){enhance();new MutationObserver(()=>requestAnimationFrame(enhance)).observe(document.body,{childList:true,subtree:true,attributes:true,attributeFilter:['class']})}
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',init);else init();
})();