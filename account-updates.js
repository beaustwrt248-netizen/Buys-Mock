(()=>{
 const q=(s,r=document)=>r.querySelector(s),qa=(s,r=document)=>[...r.querySelectorAll(s)];
 let jumpToUpdates=false;
 function status(el,text){if(el)el.textContent=text}
 async function swState(section){const el=q('#morleyAccountSwState',section);if(!('serviceWorker'in navigator)){status(el,'Not supported');return}try{const reg=await navigator.serviceWorker.getRegistration();status(el,reg?`Registered${reg.waiting?' · update waiting':''}`:'Not registered')}catch{status(el,'Unavailable')}}
 async function checkUpdates(section){const out=q('#morleyAccountUpdateStatus',section),btn=q('#morleyAccountCheckUpdates',section);if(btn)btn.disabled=true;status(out,'Checking for updates…');try{if(!('serviceWorker'in navigator)){status(out,'Service workers are not supported in this browser.');return}const reg=await navigator.serviceWorker.getRegistration();if(!reg){status(out,'No active service worker is registered.');return}await reg.update();status(out,reg.waiting?'An update is ready. Use Update App to refresh.':'Update check completed. The current deployment is active.');await swState(section)}catch(e){status(out,`Update check failed${e?.message?`: ${e.message}`:''}`)}finally{if(btn)btn.disabled=false}}
 async function updateApp(section){const out=q('#morleyAccountUpdateStatus',section),btn=q('#morleyAccountUpdateApp',section);if(btn)btn.disabled=true;status(out,'Refreshing the app from the current deployment…');try{if('serviceWorker'in navigator){const reg=await navigator.serviceWorker.getRegistration();if(reg)await reg.unregister()}if('caches'in window){const names=await caches.keys();await Promise.all(names.map(name=>caches.delete(name)))}const url=new URL(location.href);url.searchParams.set('_morley_update',Date.now().toString());location.replace(url.toString())}catch(e){status(out,`Update refresh failed${e?.message?`: ${e.message}`:''}`);if(btn)btn.disabled=false}}
 function bind(section){const check=q('#morleyAccountCheckUpdates',section),update=q('#morleyAccountUpdateApp',section);if(check&&!check.dataset.bound){check.dataset.bound='1';check.addEventListener('click',()=>checkUpdates(section))}if(update&&!update.dataset.bound){update.dataset.bound='1';update.addEventListener('click',()=>updateApp(section))}swState(section)}
 function sync(){
  qa('[data-action="updates"]').forEach(el=>{el.hidden=true;el.setAttribute('aria-hidden','true')});
  const d=q('#morleyMenuDialog.open');
  if(!d||q('h2',d)?.textContent.trim()!=='Account & Profile')return;
  const body=q('.morley-settings-body',d);if(!body)return;
  let section=q('#morleyAccountUpdates',d);
  if(!section){
   section=document.createElement('section');section.id='morleyAccountUpdates';
   section.innerHTML='<h3>Updates</h3><p>B&L Morley keeps itself current automatically. Use the controls below if you want to refresh immediately.</p><p>Current web build: <b>'+String(document.lastModified||'current deployment')+'</b></p><p>Service worker: <b id="morleyAccountSwState">Checking…</b></p><div style="display:flex;gap:8px;flex-wrap:wrap"><button type="button" id="morleyAccountUpdateApp">Update App</button><button type="button" id="morleyAccountCheckUpdates" class="secondary">Check for updates</button></div><p id="morleyAccountUpdateStatus" role="status" aria-live="polite"></p>';
   body.appendChild(section);
  }
  bind(section);
  if(jumpToUpdates){jumpToUpdates=false;section.scrollIntoView({block:'start'})}
 }
 function openAccountUpdates(){const account=q('[data-action="account"]');if(!account)return;jumpToUpdates=true;account.click()}
 function legacyRoute(){const hash=(location.hash||'').toLowerCase();if(hash==='#updates'||hash==='#account-updates')openAccountUpdates()}
 function init(){sync();legacyRoute();new MutationObserver(sync).observe(document.body,{childList:true,subtree:true,attributes:true,attributeFilter:['class']});window.addEventListener('hashchange',legacyRoute)}
 if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',init);else init();
})();