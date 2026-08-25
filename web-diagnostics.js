(()=>{
 const $=(s,r=document)=>r.querySelector(s);
 const SB='https://ghdhairijqjqivqriigi.supabase.co';
 const KEY='sb_publishable_ch49o8WRnDb8pPzowZH3Tg_XZcIbgvt';
 function esc(v){return String(v??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]))}
 function auth(){try{return JSON.parse(localStorage.getItem('morley_web_auth')||'null')}catch{return null}}
 function localStorageReady(){try{const k='__morley_diag__';localStorage.setItem(k,'1');localStorage.removeItem(k);return true}catch{return false}}
 function statusRow(label,value,ok,pending=false){const mark=pending?'…':ok?'●':'!';const color=pending?'#16c7ff':ok?'#57e389':'#ff8a9b';return `<div style="display:flex;gap:12px;align-items:flex-start;padding:12px 0;border-bottom:1px solid rgba(142,166,196,.13)"><span style="color:${color};font-weight:900">${mark}</span><div><b style="color:#fff">${esc(label)}</b><div style="margin-top:3px;color:#8ea6c4;font-size:12px;line-height:1.45">${esc(value)}</div></div></div>`}
 async function backendReady(){try{const r=await fetch(`${SB}/rest/v1/`,{method:'HEAD',headers:{apikey:KEY},cache:'no-store'});return {ok:r.ok||r.status===400||r.status===404,detail:`Supabase responded with HTTP ${r.status}`}}catch(e){return {ok:false,detail:`Backend check failed: ${e?.message||'network error'}`}}}
 function localRows(){
  const session=auth();
  const notify=('Notification'in window)?Notification.permission:'unsupported';
  const storage=localStorageReady();
  return [
   ['Web build',document.lastModified||'Current production deployment',true],
   ['Account session',session?.access_token?'Authenticated browser session is present':'No active browser session',!!session?.access_token],
   ['Internet connection',navigator.onLine?'Browser reports online':'Browser reports offline',navigator.onLine],
   ['Notifications',notify==='granted'?'Browser notifications are enabled':notify==='unsupported'?'Browser notifications are not supported':`Notification permission: ${notify}`,notify==='granted'||notify==='unsupported'],
   ['Local workspace',storage?'Browser storage is available':'Browser storage is unavailable',storage],
   ['Platform',navigator.userAgent,true]
  ];
 }
 async function renderInto(d){
  const body=$('.morley-settings-body',d);if(!body)return;
  body.innerHTML=`<p style="color:#9db0c9;line-height:1.55">Read-only checks for the web workspace. Diagnostics do not modify valuation, inventory, sales or account data.</p>${localRows().map(x=>statusRow(...x)).join('')}${statusRow('Pricing/backend service','Checking live service reachability…',false,true)}<div id="morleyDiagLive"></div><button id="morleyDiagRefresh" style="width:100%;margin-top:14px;padding:12px;border:1px solid #2f7cff;border-radius:12px;background:linear-gradient(120deg,#1f5fd8,#2f7cff,#12c9ff);color:#fff;font-weight:900;cursor:pointer">Refresh Diagnostics</button><div id="morleyDiagChecked" style="margin-top:10px;color:#7087a6;font-size:11px"></div>`;
  $('#morleyDiagRefresh',d).onclick=()=>renderInto(d);
  const result=await backendReady();
  const live=$('#morleyDiagLive',d);if(live)live.innerHTML=statusRow('Pricing/backend service',result.detail,result.ok);
  const checked=$('#morleyDiagChecked',d);if(checked)checked.textContent=`Last checked: ${new Date().toLocaleString()}`;
 }
 function open(){
  let d=$('#morleyWebDiagnostics');if(!d){d=document.createElement('div');d.id='morleyWebDiagnostics';d.className='morley-menu-dialog';document.body.appendChild(d)}
  d.innerHTML='<div class="morley-menu-dialog-card" style="max-height:82vh;overflow:auto"><button class="morley-menu-dialog-close">×</button><h2>System Diagnostics</h2><div class="morley-settings-body"></div></div>';
  d.classList.add('open');
  $('.morley-menu-dialog-close',d).onclick=()=>d.classList.remove('open');
  d.onclick=e=>{if(e.target===d)d.classList.remove('open')};
  renderInto(d);
 }
 function addRow(){
  const root=$('#morleyRelevantMore');if(!root||$('#morleyWebDiagnosticsRow'))return;
  const about=[...root.querySelectorAll('.morley-menu-row')].find(b=>(b.textContent||'').includes('About B&L Morley'));
  if(!about)return;
  const row=document.createElement('button');row.id='morleyWebDiagnosticsRow';row.className='morley-menu-row';row.type='button';row.innerHTML='<span class="morley-menu-icon">◇</span><span class="morley-menu-copy"><b>System Diagnostics</b><small>Check browser, account, network, backend and storage readiness.</small></span><span class="morley-menu-chevron">›</span>';
  row.onclick=open;about.parentElement.insertBefore(row,about);
 }
 function init(){addRow();let queued=false;new MutationObserver(()=>{if(queued)return;queued=true;requestAnimationFrame(()=>{queued=false;addRow()})}).observe(document.body,{childList:true,subtree:true})}
 if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',init);else init();
})();
