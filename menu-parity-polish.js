(()=>{
  const AUTH='morley_web_auth';
  const $=(s,r=document)=>r.querySelector(s);

  function safeStorageSnapshot(){
    let count=0, bytes=0;
    try{
      for(let i=0;i<localStorage.length;i++){
        const k=localStorage.key(i);
        if(!k||k===AUTH) continue;
        const v=localStorage.getItem(k)||'';
        count++; bytes+=k.length+v.length;
      }
    }catch{}
    return {count,bytes};
  }

  function sessionState(){
    try{
      const s=JSON.parse(localStorage.getItem(AUTH)||'null');
      if(!s?.access_token) return 'Signed out';
      const p=s.access_token.split('.')[1].replace(/-/g,'+').replace(/_/g,'/');
      const payload=JSON.parse(atob(p+'='.repeat((4-p.length%4)%4)));
      if(payload.exp && Date.now()/1000>=payload.exp) return 'Expired';
      return 'Active';
    }catch{return 'Unknown'}
  }

  function notificationState(){
    if(!('Notification' in window)) return 'Unsupported by this browser';
    return Notification.permission==='granted'?'Allowed':Notification.permission==='denied'?'Blocked':'Not requested';
  }

  function button(id,text,danger=false){
    return `<button id="${id}" type="button" style="width:100%;margin-top:12px;padding:12px;border-radius:12px;border:1px solid ${danger?'rgba(255,90,110,.5)':'rgba(47,124,255,.35)'};background:${danger?'rgba(91,26,38,.72)':'#0a1b33'};color:${danger?'#ffb1bd':'#fff'};font-weight:900;cursor:pointer">${text}</button>`;
  }

  function enhanceBackup(d){
    const body=$('.morley-settings-body',d); if(!body||$('#morleySafeClear',d)) return;
    const holder=document.createElement('div');
    holder.innerHTML=`<div style="margin-top:18px;padding-top:14px;border-top:1px solid rgba(47,124,255,.18)"><b style="color:#fff">Local workspace controls</b><p style="color:#9db0c9;line-height:1.5;font-size:12px">Clear this browser's local Morley workspace and preferences without deleting your authorised sign-in session.</p>${button('morleySafeClear','Clear Local Workspace Data',true)}</div>`;
    body.appendChild(holder);
    $('#morleySafeClear',d).onclick=()=>{
      const b=$('#morleySafeClear',d);
      if(b.dataset.confirm!=='yes'){
        b.dataset.confirm='yes'; b.textContent='Tap again to confirm clear';
        setTimeout(()=>{ if(b?.dataset.confirm==='yes'){b.dataset.confirm='';b.textContent='Clear Local Workspace Data'} },6000);
        return;
      }
      const auth=localStorage.getItem(AUTH);
      const keys=[];
      for(let i=0;i<localStorage.length;i++){const k=localStorage.key(i);if(k&&k!==AUTH)keys.push(k)}
      keys.forEach(k=>localStorage.removeItem(k));
      if(auth!==null)localStorage.setItem(AUTH,auth);
      b.dataset.confirm=''; b.textContent='Local workspace cleared'; b.disabled=true;
    };
  }

  function enhanceNotifications(d){
    const body=$('.morley-settings-body',d); if(!body||$('#morleyNotificationStatus',d)) return;
    const box=document.createElement('div');
    box.id='morleyNotificationStatus';
    box.style.cssText='margin:12px 0;padding:12px;border:1px solid rgba(18,201,255,.2);border-radius:12px;background:rgba(5,18,36,.65);color:#dce9ff;font-size:12px;line-height:1.55';
    box.innerHTML=`<b style="color:#fff">Browser permission</b><br><span style="color:#9db0c9">Current status:</span> ${notificationState()}<br><span style="color:#9db0c9">Preferences:</span> Update alerts and valuation/error alerts are stored locally for this browser.`;
    body.prepend(box);
  }

  function enhancePrivacy(d){
    const body=$('.morley-settings-body',d); if(!body||$('#morleySessionSnapshot',d)) return;
    const box=document.createElement('div');
    box.id='morleySessionSnapshot';
    box.style.cssText='margin:12px 0;padding:12px;border:1px solid rgba(18,201,255,.2);border-radius:12px;background:rgba(5,18,36,.65);color:#dce9ff;font-size:12px;line-height:1.55';
    box.innerHTML=`<b style="color:#fff">Session snapshot</b><br><span style="color:#9db0c9">Local session:</span> ${sessionState()}<br><span style="color:#9db0c9">Connection:</span> ${navigator.onLine?'Online':'Offline'}`;
    body.prepend(box);
  }

  function enhanceAbout(d){
    const body=$('.morley-settings-body',d); if(!body||$('#morleyDiagnosticsSnapshot',d)) return;
    const s=safeStorageSnapshot();
    const box=document.createElement('div'); box.id='morleyDiagnosticsSnapshot';
    box.style.cssText='margin-top:16px;padding:14px;border:1px solid rgba(18,201,255,.22);border-radius:14px;background:rgba(5,18,36,.72);color:#dce9ff;line-height:1.65';
    box.innerHTML=`<b style="color:#fff">Diagnostics snapshot</b><br><span style="color:#9db0c9">Network:</span> ${navigator.onLine?'Online':'Offline'}<br><span style="color:#9db0c9">Secure session:</span> ${sessionState()}<br><span style="color:#9db0c9">Notifications:</span> ${notificationState()}<br><span style="color:#9db0c9">Local workspace:</span> ${s.count} keys · ~${Math.max(1,Math.round(s.bytes/1024))} KB<br><span style="color:#9db0c9">Browser:</span> ${navigator.userAgent.replace(/</g,'&lt;').slice(0,150)}`;
    body.appendChild(box);
  }

  function improveMessages(d){
    const msg=$('.morley-settings-msg',d); if(msg){msg.setAttribute('role','status');msg.setAttribute('aria-live','polite')}
  }

  function inspect(){
    const d=$('#morleyMenuDialog.open'); if(!d) return;
    const title=$('h2',d)?.textContent?.trim();
    if(title==='Backup & Data') enhanceBackup(d);
    if(title==='Notifications') enhanceNotifications(d);
    if(title==='Privacy & Security') enhancePrivacy(d);
    if(title==='About B&L Morley') enhanceAbout(d);
    improveMessages(d);
  }

  function init(){
    inspect();
    const obs=new MutationObserver(()=>requestAnimationFrame(inspect));
    obs.observe(document.body,{childList:true,subtree:true,attributes:true,attributeFilter:['class']});
    window.addEventListener('online',inspect); window.addEventListener('offline',inspect);
  }
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',init);else init();
})();