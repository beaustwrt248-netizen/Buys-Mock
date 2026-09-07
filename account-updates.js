(()=>{
 const q=(s,r=document)=>r.querySelector(s),qa=(s,r=document)=>[...r.querySelectorAll(s)];
 let jumpToUpdates=false;
 function sync(){
  qa('[data-action="updates"]').forEach(el=>{el.hidden=true;el.setAttribute('aria-hidden','true')});
  const d=q('#morleyMenuDialog.open');
  if(!d||q('h2',d)?.textContent.trim()!=='Account & Profile')return;
  const body=q('.morley-settings-body',d);if(!body)return;
  if(!q('#morleyAccountUpdates',d)){
   const section=document.createElement('section');section.id='morleyAccountUpdates';
   section.innerHTML='<h3>Updates</h3><p>App and web update status now lives in Account.</p><p>Web build: <b>'+String(document.lastModified||'current deployment')+'</b></p>';
   body.appendChild(section);
  }
  if(jumpToUpdates){jumpToUpdates=false;q('#morleyAccountUpdates',d)?.scrollIntoView({block:'start'})}
 }
 function openAccountUpdates(){const account=q('[data-action="account"]');if(!account)return;jumpToUpdates=true;account.click()}
 function legacyRoute(){const hash=(location.hash||'').toLowerCase();if(hash==='#updates'||hash==='#account-updates')openAccountUpdates()}
 function init(){sync();legacyRoute();new MutationObserver(sync).observe(document.body,{childList:true,subtree:true,attributes:true,attributeFilter:['class']});window.addEventListener('hashchange',legacyRoute)}
 if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',init);else init();
})();