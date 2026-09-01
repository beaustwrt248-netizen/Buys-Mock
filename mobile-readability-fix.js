(()=>{
'use strict';
const MOBILE='(max-width:760px)';
const TARGETS=new Set(['LIVE PRICING','ONLINE STATUS']);
let queued=false,observer=null;

function ensureStyles(){
  if(document.getElementById('morleyMobileReadabilityFix'))return;
  const style=document.createElement('style');
  style.id='morleyMobileReadabilityFix';
  style.textContent=`
@media(max-width:760px){
  html,body{width:100%!important;max-width:100%!important;min-width:0!important;overflow-x:hidden!important}
  body{zoom:1!important}
  main.app,.app{box-sizing:border-box!important;width:100%!important;max-width:100vw!important;min-width:0!important;margin-left:0!important;margin-right:0!important;transform:none!important;zoom:1!important}
  .section{box-sizing:border-box!important;width:100%!important;max-width:100%!important;min-width:0!important;transform:none!important}
  #home{overflow-x:hidden!important}
  .morley-mobile-status-readable{color:#1c2b26!important;opacity:1!important;filter:none!important}
  .morley-mobile-status-readable *{opacity:1!important;filter:none!important}
  .morley-mobile-status-readable-label{display:block!important;color:#263a34!important;opacity:1!important;font-size:12px!important;line-height:1.25!important;font-weight:900!important;letter-spacing:.08em!important;text-shadow:none!important}
  .morley-mobile-status-readable-value{color:#17372d!important;opacity:1!important;font-size:14px!important;line-height:1.35!important;font-weight:800!important;text-shadow:none!important}
}
`;
  document.head.appendChild(style);
}

function markStatusCards(){
  if(!window.matchMedia(MOBILE).matches)return;
  const all=[...document.querySelectorAll('#home *')];
  for(const el of all){
    const text=(el.textContent||'').trim().replace(/\s+/g,' ').toUpperCase();
    if(!TARGETS.has(text))continue;
    el.classList.add('morley-mobile-status-readable-label');
    const card=el.closest('.card,.tile,.metric,[class*="card"],[class*="tile"],[class*="status"]')||el.parentElement;
    if(!card)continue;
    card.classList.add('morley-mobile-status-readable');
    [...card.children].forEach(child=>{
      if(child!==el)child.classList.add('morley-mobile-status-readable-value');
    });
  }
}

function apply(){ensureStyles();markStatusCards();}
function schedule(){if(queued)return;queued=true;(window.requestAnimationFrame||window.setTimeout)(()=>{queued=false;apply()});}
function boot(){
  apply();
  observer=new MutationObserver(schedule);
  observer.observe(document.body,{subtree:true,childList:true,attributes:true,attributeFilter:['class','style']});
  window.addEventListener('resize',schedule,{passive:true});
  window.addEventListener('morley-product-parity-ready',schedule);
  window.addEventListener('morley-initial-layout-ready',schedule);
}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot,{once:true});else boot();
})();
