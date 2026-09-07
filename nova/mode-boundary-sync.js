(()=>{'use strict';
function text(el,value){if(el&&el.textContent!==value)el.textContent=value}
function sync(){
  const metrics=[...document.querySelectorAll('.metric')];
  const mode=metrics.find(card=>card.querySelector('span')?.textContent.trim()==='Mode');
  if(mode){text(mode.querySelector('strong'),'GUARDED');text(mode.querySelector('small'),'allow-listed audited actions')}
  const boundaries=[...document.querySelectorAll('.boundary-list > div')];
  for(const row of boundaries){const label=row.querySelector('b')?.textContent.trim(),value=row.querySelector('span');if(label==='Production writes')text(value,'Catalogue review queue/findings only');if(label==='Auth & permissions')text(value,'Admin-only; changes prohibited')}
  const summary=document.getElementById('coreSummary');
  if(summary&&/Reading the current Buys-Mock development state without changing production systems\./.test(summary.textContent||''))text(summary,'Operating with guarded intelligence and narrowly allow-listed, auditable review actions. Protected authority remains human-gated.')
  const footer=document.querySelector('footer');
  if(footer&&footer.textContent.includes('standalone foundation'))text(footer,'© 2026 Morley Buys · Nova AI guarded control centre · Guardian protected')
}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',sync,{once:true});else sync();
window.addEventListener('nova:authenticated',sync);
window.addEventListener('nova:refreshed',sync);
window.NovaModeBoundarySync={sync};
})();
