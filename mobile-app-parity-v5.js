(()=>{
'use strict';
const $=(s,r=document)=>r.querySelector(s),$$=(s,r=document)=>[...r.querySelectorAll(s)];
const mobile=()=>document.documentElement.classList.contains('morley-physical-phone')||matchMedia('(max-width:760px)').matches;
const NAV_SIGNATURE='home|categories|general|settings';
const CATEGORY_IDS=new Set(['categories','computer','laptop','desktop','mobilePhones','console','universalBuySearch']);
let queued=false,bodyObserver=null,headObserver=null;
function mobileShow(page){
 const target=$('#'+page);
 if(typeof window.show==='function')window.show(page);
 if(target&&!target.classList.contains('active')){
  $$('.section').forEach(sec=>sec.classList.toggle('active',sec===target));
 }
 if(target)window.scrollTo({top:0,behavior:'smooth'});
}
const go=page=>{if(mobile())mobileShow(page);else if(typeof window.morleyDesktopGo==='function')window.morleyDesktopGo(page);else window.show?.(page)};

function markOwner(){
 if(!document.body.id)document.body.id='morleyMobileParityRoot';
 document.documentElement.dataset.morleyMobileAppParity='5';
}
function ensureStyleAuthority(){
 const link=$('link[data-morley-mobile-app-parity="5"]');
 if(link&&link.parentNode===document.head&&link!==document.head.lastElementChild)document.head.appendChild(link);
 $('#morleyMobileLayoutFinal')?.remove();
}
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
function navSignature(nav){return $$('button[data-page]',nav).map(b=>b.dataset.page).join('|')}
function navButton(page,label,icon){return `<button class="morley-bottom-nav-item" data-page="${page}" type="button" aria-label="${label}"><span class="morley-nav-icon" aria-hidden="true">${icon}</span><small>${label}</small></button>`}
function ensureNav(){
 const nav=$('body>nav')||$('nav[aria-label="Primary navigation"]');if(!nav)return;
 nav.id='morleyBottomNav';nav.dataset.morleyAndroidOwner='v5';nav.classList.remove('morley-mobile-overlay-active');nav.style.removeProperty('display');nav.style.removeProperty('background');nav.style.removeProperty('background-image');nav.style.removeProperty('grid-template-columns');
 const correct=navSignature(nav)===NAV_SIGNATURE&&$$('button[data-page]',nav).every(b=>b.classList.contains('morley-bottom-nav-item'));
 if(!correct){
  nav.innerHTML=navButton('home','Home','⌂')+navButton('categories','Categories','▦')+navButton('general','General Buys','$')+navButton('settings','More','•••');
  $$('button[data-page]',nav).forEach(b=>b.addEventListener('click',()=>go(b.dataset.page)));
 }
 $$('button[data-page]',nav).forEach(b=>{b.classList.remove('morley-hide-more-tab');b.removeAttribute('style')});
 syncNav();
}
function syncNav(){
 const nav=$('#morleyBottomNav')||$('body>nav');if(!nav)return;const active=$('.section.active')?.id||'home';let parent='home';if(CATEGORY_IDS.has(active))parent='categories';else if(active==='general')parent='general';else if(active==='settings')parent='settings';$$('button[data-page]',nav).forEach(b=>b.classList.toggle('active',b.dataset.page===parent));
}
function ensureMenuTrigger(){
 const trigger=$('#morleyMenuTrigger');if(!trigger)return;trigger.classList.add('morley-menu-trigger-fixed');trigger.style.removeProperty('background');trigger.style.removeProperty('background-image');trigger.style.removeProperty('box-shadow');trigger.style.removeProperty('color');const label=$('.morley-menu-trigger-label',trigger);if(label)label.textContent='Menu';
}
function compactNfc(){
 const root=$('#morleySmartWorkspace');if(!root)return;const old=$('#morleyWebNfcInfo',root);if(old&&old.tagName==='BUTTON'){const note=document.createElement('div');note.id='morleyWebNfcInfo';note.className='morley-web-nfc-note';note.setAttribute('role','note');note.innerHTML='<span aria-hidden="true">⌁</span><span>NFC scanning is available in the Android app.</span>';old.replaceWith(note)}
}
function canonicalActions(){
 const root=$('#morleySmartWorkspace');if(!root)return;const quick=$('#mswNewDeal',root),open=$('#mswOpenDeals',root);if(quick){quick.textContent='Quick Deal Mode';quick.removeAttribute('style')}if(open){open.textContent='Open Valuations & Deals';open.removeAttribute('style')}
}
function alignWorkspace(){
 const root=$('#morleySmartWorkspace');if(!root)return;
 const head=$('.msw-head',root),grid=$('.msw-grid',root),margin=$('.morley-potential-margin',root),actions=$('.msw-actions',root),recent=$('.msw-recent',root),open=$('#mswOpenDeals',root),refresh=$('#morleyHomeRefresh',root);
 if(open&&actions?.contains(open))root.appendChild(open);
 const ordered=[head,grid,margin,actions,recent,...$$('.mswp-panel',root),open,refresh].filter(Boolean);
 ordered.forEach(node=>root.appendChild(node));
}
function ensureHomeExtras(){
 const home=$('#home'),root=$('#morleySmartWorkspace');if(!home||!root)return;
 let extras=$('#morleyAndroidHomeExtras',home);
 if(!extras){extras=document.createElement('div');extras.id='morleyAndroidHomeExtras';extras.innerHTML=`<div class="morley-home-status-grid"><div class="morley-home-status-tile"><small>LIVE PRICING</small><b>READY</b></div><div class="morley-home-status-tile"><small>ONLINE STATUS</small><b data-online-status>ONLINE</b></div></div><button class="morley-home-gp-card" type="button"><span class="morley-home-gp-icon" aria-hidden="true">$</span><span><b>General Buys / GP</b><small>A / B / C / Luxury buying targets</small></span><span class="morley-home-gp-chevron" aria-hidden="true">›</span></button>`;root.insertAdjacentElement('afterend',extras);$('.morley-home-gp-card',extras).addEventListener('click',()=>go('general'))}
 const online=$('[data-online-status]',extras);if(online)online.textContent=navigator.onLine===false?'OFFLINE':'ONLINE';
}
function apply(){
 if(!mobile())return;markOwner();ensureStyleAuthority();ensureCategories();ensureCategoryBack();ensureNav();ensureMenuTrigger();compactNfc();canonicalActions();alignWorkspace();ensureHomeExtras();document.body.classList.remove('morley-mobile-overlay-active');
}
function schedule(){if(queued)return;queued=true;(requestAnimationFrame||setTimeout)(()=>{queued=false;apply()})}
function boot(){
 apply();
 bodyObserver=new MutationObserver(records=>{if(records.some(r=>r.type==='childList'||r.attributeName==='class'||r.attributeName==='style'||r.attributeName==='data-page'))schedule()});
 bodyObserver.observe(document.body,{childList:true,subtree:true,attributes:true,attributeFilter:['class','style','data-page']});
 headObserver=new MutationObserver(schedule);headObserver.observe(document.head,{childList:true});
 ['morley-product-parity-ready','morley-initial-layout-ready','morley-universal-buy-ready','morley-valuation-history-updated','morley-watchlist-updated','online','offline','resize','orientationchange'].forEach(e=>window.addEventListener(e,schedule,{passive:true}));
 window.visualViewport?.addEventListener('resize',schedule,{passive:true});
}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot,{once:true});else boot();
})();
