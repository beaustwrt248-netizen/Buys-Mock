(()=>{
'use strict';
const $=(s,r=document)=>r.querySelector(s),$$=(s,r=document)=>[...r.querySelectorAll(s)];
const icons={
 home:'<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M3 10.8 12 3l9 7.8v9.7a.5.5 0 0 1-.5.5h-5.7v-6.2H9.2V21H3.5a.5.5 0 0 1-.5-.5v-9.7Z"/></svg>',
 computer:'<svg viewBox="0 0 24 24" aria-hidden="true"><rect x="3" y="4" width="18" height="12" rx="2"/><path d="M8 20h8M12 16v4"/></svg>',
 console:'<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M7.2 8h9.6c2.2 0 3.9 1.6 4.2 3.7l.7 4.6c.3 2-1.9 3.4-3.5 2.2l-2.4-1.8H8.2l-2.4 1.8c-1.6 1.2-3.8-.2-3.5-2.2l.7-4.6C3.3 9.6 5 8 7.2 8Z"/><path d="M7 11v4M5 13h4M16.5 12.2h.01M18.5 14h.01"/></svg>',
 gp:'<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 2v20M17 6.2c-1.1-1-2.7-1.7-4.6-1.7-2.8 0-4.9 1.4-4.9 3.5 0 5.3 9.5 2.4 9.5 7.5 0 2.3-2.1 4-5.2 4-2.2 0-4.1-.8-5.3-2"/></svg>'
};
function go(page){if(typeof window.morleyDesktopGo==='function')window.morleyDesktopGo(page);else if(typeof window.show==='function')window.show(page);}
function navButton(page,label,icon){return `<button data-page="${page}" type="button" aria-label="${label}"><span class="morley-nav-icon">${icons[icon||page]}</span><small>${label}</small></button>`;}
function fixNav(){
 const nav=$('nav');if(!nav)return;
 const signature=$$('button[data-page]',nav).map(b=>b.dataset.page).join('|');
 if(signature!=='home|computer|console|general'||!nav.querySelector('.morley-nav-icon')){
  nav.innerHTML=navButton('home','Home')+navButton('computer','Computer')+navButton('console','Console')+navButton('general','GP','gp');
  $$('button[data-page]',nav).forEach(b=>b.addEventListener('click',()=>go(b.dataset.page)));
 }
 nav.style.gridTemplateColumns='repeat(4,minmax(0,1fr))';
 const active=$('.section.active')?.id||'home';
 const mapped=(active==='laptop'||active==='desktop')?'computer':active;
 $$('button[data-page]',nav).forEach(b=>b.classList.toggle('active',b.dataset.page===mapped));
}
function styles(){if($('#morleyDesktopModeLayoutFix'))return;const s=document.createElement('style');s.id='morleyDesktopModeLayoutFix';s.textContent=`
.morley-home-tile-icon,.morley-choice-icon{display:grid!important;place-items:center!important;width:32px!important;height:32px!important;min-width:32px!important;min-height:32px!important;color:#eafff8!important;flex:0 0 auto!important}
.morley-home-tile-icon svg,.morley-choice-icon svg{display:block!important;width:32px!important;height:32px!important;max-width:32px!important;max-height:32px!important;fill:none!important;stroke:currentColor!important;stroke-width:1.9!important;stroke-linecap:round!important;stroke-linejoin:round!important}
body nav .morley-nav-icon{display:grid!important;place-items:center!important;width:24px!important;height:24px!important;min-width:24px!important;min-height:24px!important;color:inherit!important}
body nav .morley-nav-icon svg{display:block!important;width:24px!important;height:24px!important;max-width:24px!important;max-height:24px!important;fill:none!important;stroke:currentColor!important;stroke-width:1.9!important;stroke-linecap:round!important;stroke-linejoin:round!important}
body nav button[data-page="desktop"]{display:none!important}
`;document.head.appendChild(s);}
let queued=false;function apply(){styles();fixNav();}
function schedule(){if(queued)return;queued=true;(requestAnimationFrame||setTimeout)(()=>{queued=false;apply();});}
function boot(){apply();const nav=$('nav');if(nav)new MutationObserver(schedule).observe(nav,{childList:true,subtree:true});const main=$('main.app')||$('main');if(main)new MutationObserver(schedule).observe(main,{subtree:true,attributes:true,attributeFilter:['class']});addEventListener('morley-product-parity-ready',schedule);addEventListener('resize',schedule,{passive:true});}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot,{once:true});else boot();
})();
