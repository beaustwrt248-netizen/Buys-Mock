(()=>{
'use strict';
const $=(s,r=document)=>r.querySelector(s),$$=(s,r=document)=>[...r.querySelectorAll(s)];
let busy=false;
function cleanSettings(){
 const sec=$('#settings');
 const root=$('#morleyRelevantMore',sec||document);
 if(!sec||!root)return;
 for(const child of [...sec.children]){
   if(child===root)continue;
   child.classList.add('morley-legacy-menu-hidden');
   child.setAttribute('aria-hidden','true');
 }
 // Also catch late-injected legacy management/dashboard blocks that can be nested.
 for(const el of $$('section,article,div',sec)){
   if(el===root||root.contains(el))continue;
   const t=(el.textContent||'').replace(/\s+/g,' ').trim();
   if(/\bManagement\b/i.test(t)&&/\bInventory\b/i.test(t)&&/\bSaved Deals\b/i.test(t)){
     el.classList.add('morley-legacy-menu-hidden');
     el.setAttribute('aria-hidden','true');
   }
 }
 document.documentElement.dataset.morleyMenuCanonical='1';
}
function schedule(){if(busy)return;busy=true;(window.requestAnimationFrame||setTimeout)(()=>{busy=false;cleanSettings()});}
function boot(){cleanSettings();const obs=new MutationObserver(schedule);obs.observe(document.body,{childList:true,subtree:true});window.addEventListener('morley-product-parity-ready',schedule);window.addEventListener('morley-initial-layout-ready',schedule);}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot,{once:true});else boot();
})();
