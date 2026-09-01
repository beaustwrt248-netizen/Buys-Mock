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
body nav button::before,body nav button::after{content:none!important;display:none!important}
body nav button[data-page="desktop"]{display:none!important}
@media (min-width:1000px){
  #morleyDesktopShell{box-sizing:border-box!important;transform:translateX(calc(-100% - 2px))!important;visibility:hidden!important;pointer-events:none!important;transition:transform .2s ease,visibility 0s linear .2s!important}
  body.morley-desktop-menu-open #morleyDesktopShell{transform:translateX(0)!important;visibility:visible!important;pointer-events:auto!important;transition:transform .2s ease!important}
  #morleyDesktopMenuScrim{display:none;position:fixed;inset:0;z-index:39;border:0;background:rgba(0,0,0,.48);cursor:default}
  body.morley-desktop-menu-open #morleyDesktopMenuScrim{display:block}
  body.morley-desktop-menu-open .app{box-sizing:border-box!important;width:auto!important;max-width:calc(100vw - 248px)!important;margin-left:248px!important;margin-right:0!important}
}
@media (min-width:761px) and (max-width:1100px){
  body{font-size:16px!important}
  main.app,.app{padding-left:18px!important;padding-right:18px!important;padding-bottom:104px!important}
  .top{min-height:68px!important;padding-top:10px!important;padding-bottom:10px!important}
  #home .card,#home .msw-card,#home .mswp-card{margin-bottom:14px!important}
  #home .muted,#home p,#home small,.card .muted,.card small{line-height:1.5!important}
  #home .muted,.card .muted{color:#aebbb7!important;opacity:1!important}
  #home .tiles{gap:14px!important}
  #home .tiles button{min-height:170px!important;padding:20px!important}
  #home .tiles button b{font-size:18px!important;line-height:1.25!important}
  #home .tiles button small{font-size:13px!important;line-height:1.45!important;color:rgba(255,255,255,.86)!important;opacity:1!important}
  .morley-home-tile-icon{width:34px!important;height:34px!important;min-width:34px!important;min-height:34px!important}
  .morley-home-tile-icon svg{width:34px!important;height:34px!important;max-width:34px!important;max-height:34px!important}
  body nav{left:16px!important;right:16px!important;bottom:12px!important;width:auto!important;grid-template-columns:repeat(4,minmax(0,1fr))!important;gap:8px!important;padding:8px!important;border-radius:18px!important}
  body nav button,body nav button.active{min-height:66px!important;height:66px!important;display:flex!important;flex-direction:column!important;align-items:center!important;justify-content:center!important;gap:5px!important;font-size:0!important}
  body nav button small{display:block!important;font-size:12px!important;line-height:14px!important;font-weight:800!important;color:inherit!important}
  body nav .morley-nav-icon{width:25px!important;height:25px!important;min-width:25px!important;min-height:25px!important}
  body nav .morley-nav-icon svg{width:25px!important;height:25px!important;max-width:25px!important;max-height:25px!important}
  .morley-web-user{font-size:12px!important}
}
`;document.head.appendChild(s);}
function desktopMenu(){
 const trigger=$('#morleyMenuTrigger'),shell=$('#morleyDesktopShell');if(!trigger||!shell)return;
 let scrim=$('#morleyDesktopMenuScrim');
 if(!scrim){scrim=document.createElement('button');scrim.id='morleyDesktopMenuScrim';scrim.type='button';scrim.tabIndex=-1;scrim.setAttribute('aria-label','Close B&L Morley menu');document.body.insertBefore(scrim,shell);}
 const setOpen=open=>{document.body.classList.toggle('morley-desktop-menu-open',open);trigger.setAttribute('aria-expanded',String(open));shell.setAttribute('aria-hidden',String(!open));};
 if(trigger.dataset.desktopMenuBound!=='1'){
  trigger.dataset.desktopMenuBound='1';trigger.setAttribute('aria-controls',shell.id);trigger.setAttribute('aria-expanded','false');
  trigger.addEventListener('click',event=>{if(innerWidth<1000)return;event.preventDefault();event.stopImmediatePropagation();setOpen(!document.body.classList.contains('morley-desktop-menu-open'));},true);
  scrim.addEventListener('click',()=>setOpen(false));
  shell.addEventListener('click',event=>{if(event.target.closest('[data-target]'))setOpen(false);});
  document.addEventListener('keydown',event=>{if(event.key==='Escape')setOpen(false);});
 }
 if(innerWidth<1000)setOpen(false);
}
let queued=false;function apply(){styles();fixNav();desktopMenu();}
function schedule(){if(queued)return;queued=true;(requestAnimationFrame||setTimeout)(()=>{queued=false;apply();});}
function boot(){apply();const nav=$('nav');if(nav)new MutationObserver(schedule).observe(nav,{childList:true,subtree:true});addEventListener('morley-product-parity-ready',schedule);addEventListener('resize',schedule,{passive:true});}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot,{once:true});else boot();
})();
