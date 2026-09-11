(()=>{
'use strict';
const TARGET='admin/turnstile.html?v=5';
function refresh(frame){
  if(!frame||frame.dataset.morleyTurnstileCacheRecovered==='1')return;
  const raw=frame.getAttribute('src')||'';
  if(!/admin\/turnstile\.html\?v=4(?:$|&)/.test(raw))return;
  frame.dataset.morleyTurnstileCacheRecovered='1';
  const base=new URL(TARGET,document.baseURI);
  base.searchParams.set('web_cache_bust',String(Date.now()));
  frame.src=base.href;
}
function scan(root=document){
  if(root.nodeType===1&&root.matches?.('#waTurnstile'))refresh(root);
  root.querySelectorAll?.('#waTurnstile').forEach(refresh);
}
scan();
const observer=new MutationObserver(records=>{
  for(const record of records){
    for(const node of record.addedNodes)scan(node);
  }
});
observer.observe(document.documentElement,{childList:true,subtree:true});
setTimeout(()=>observer.disconnect(),30000);
})();
