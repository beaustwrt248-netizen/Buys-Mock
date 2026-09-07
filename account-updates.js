(()=>{
 const q=(s,r=document)=>r.querySelector(s),qa=(s,r=document)=>[...r.querySelectorAll(s)];
 function sync(){
  qa('[data-action="updates"]').forEach(el=>{el.hidden=true;el.setAttribute('aria-hidden','true')});
  const d=q('#morleyMenuDialog.open');
  if(!d||q('h2',d)?.textContent.trim()!=='Account & Profile')return;
  const body=q('.morley-settings-body',d);if(!body||q('#morleyAccountUpdates',d))return;
  const section=document.createElement('section');section.id='morleyAccountUpdates';
  section.innerHTML='<h3>Updates</h3><p>App and web update status now lives in Account.</p><p>Web build: <b>'+String(document.lastModified||'current deployment')+'</b></p>';
  body.appendChild(section);
 }
 function init(){sync();new MutationObserver(sync).observe(document.body,{childList:true,subtree:true,attributes:true,attributeFilter:['class']})}
 if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',init);else init();
})();