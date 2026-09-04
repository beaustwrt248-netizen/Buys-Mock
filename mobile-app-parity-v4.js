(()=>{
'use strict';
const $=(s,r=document)=>r.querySelector(s),$$=(s,r=document)=>[...r.querySelectorAll(s)];
const isMobile=()=>document.documentElement.classList.contains('morley-physical-phone')||matchMedia('(max-width:760px)').matches;
const go=page=>{if(typeof window.morleyDesktopGo==='function')window.morleyDesktopGo(page);else window.show?.(page)};
const categoryTargets=new Set(['categories','computer','laptop','desktop','mobilePhones','console','universalBuySearch']);
let observer=null,queued=false;
function ensureCategories(){
 if($('#categories'))return;
 const main=$('main.app')||$('main');if(!main)return;
 const sec=document.createElement('section');sec.id='categories';sec.className='section';
 sec.innerHTML=`<div class="morley-category-head"><h1>Categories</h1><p>Select a category to get started.</p></div><div class="morley-category-list"><button class="morley-category-card" data-target="laptop" type="button"><span class="morley-category-icon">▱</span><span class="morley-category-copy"><b>Laptops</b><small>Price laptops & MacBooks</small></span><span class="morley-category-chevron">›</span></button><button class="morley-category-card" data-target="desktop" type="button"><span class="morley-category-icon">▦</span><span class="morley-category-copy"><b>Desktops</b><small>Price desktop computers</small></span><span class="morley-category-chevron">›</span></button><button class="morley-category-card" data-target="mobilePhones" type="button"><span class="morley-category-icon">▯</span><span class="morley-category-copy"><b>Mobile Phones</b><small>Price mobile phones</small></span><span class="morley-category-chevron">›</span></button><button class="morley-category-card" data-target="console" type="button"><span class="morley-category-icon">⌁</span><span class="morley-category-copy"><b>Gaming Consoles</b><small>Price consoles & handhelds</small></span><span class="morley-category-chevron">›</span></button></div>`;
 main.appendChild(sec);$$('.morley-category-card',sec).forEach(b=>b.addEventListener('click',()=>go(b.dataset.target)));
}
function ensureCategoryBack(){
 ['laptop','desktop','mobilePhones','console'].forEach(id=>{const sec=$('#'+id);if(!sec||$('.morley-category-back',sec))return;const b=document.createElement('button');b.type='button';b.className='morley-category-back';b.textContent='‹  Categories';b.addEventListener('click',()=>go('categories'));sec.prepend(b)});
}
function ensureNav(){
 if(!isMobile())return;const nav=$('body>nav');if(!nav)return;
 const signature='home|categories|general|settings';
 if(nav.dataset.morleyAndroidV4!==signature){
  nav.innerHTML=`<button data-page="home" type="button"><span aria-hidden="true">⌂</span><small>Home</small></button><button data-page="categories" type="button"><span aria-hidden="true">▦</span><small>Categories</small></button><button data-page="general" type="button"><span aria-hidden="true">$</span><small>General Buys</small></button><button data-page="settings" type="button"><span aria-hidden="true">•••</span><small>More</small></button>`;
  nav.dataset.morleyAndroidV4=signature;$$('button[data-page]',nav).forEach(b=>b.addEventListener('click',()=>go(b.dataset.page)));
 }
 syncNav();
}
function syncNav(){
 const nav=$('body>nav');if(!nav)return;const active=$('.section.active')?.id||'home';let parent='home';if(categoryTargets.has(active))parent='categories';else if(active==='general')parent='general';else if(active==='settings')parent='settings';$$('button[data-page]',nav).forEach(b=>b.classList.toggle('active',b.dataset.page===parent));
}
function compactNfc(){
 const root=$('#morleySmartWorkspace');if(!root)return;const old=$('#morleyWebNfcInfo',root);if(old&&old.tagName==='BUTTON'){const note=document.createElement('div');note.id='morleyWebNfcInfo';note.className='morley-web-nfc-note';note.setAttribute('role','note');note.innerHTML='<span aria-hidden="true">⌁</span><span>NFC scanning is available in the Android app.</span>';old.replaceWith(note)}
}
function fixActions(){
 const root=$('#morleySmartWorkspace');if(!root)return;const open=$('#mswOpenDeals',root);if(open){open.textContent='Open Valuations & Deals';open.removeAttribute('style')}const quick=$('#mswNewDeal',root);if(quick){quick.textContent='Quick Deal Mode';quick.removeAttribute('style')}
}
function enforceGreen(){
 const trigger=$('#morleyMenuTrigger');if(trigger){trigger.style.removeProperty('background');trigger.style.removeProperty('background-image');trigger.style.removeProperty('color');trigger.style.removeProperty('box-shadow')}
 const nav=$('body>nav');nav?.removeAttribute('style');
}
function apply(){if(!isMobile())return;ensureCategories();ensureCategoryBack();ensureNav();compactNfc();fixActions();enforceGreen();document.documentElement.dataset.morleyMobileAppParity='4'}
function schedule(){if(queued)return;queued=true;(requestAnimationFrame||setTimeout)(()=>{queued=false;apply()})}
function boot(){apply();observer=new MutationObserver(records=>{if(records.some(r=>r.type==='childList'||r.attributeName==='class'||r.attributeName==='style'))schedule()});observer.observe(document.body,{childList:true,subtree:true,attributes:true,attributeFilter:['class','style']});['morley-product-parity-ready','morley-initial-layout-ready','morley-universal-buy-ready','resize','orientationchange'].forEach(e=>window.addEventListener(e,schedule,{passive:true}))}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot,{once:true});else boot();
})();
