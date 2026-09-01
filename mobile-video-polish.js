(()=>{
'use strict';
const $=(s,r=document)=>r.querySelector(s),$$=(s,r=document)=>[...r.querySelectorAll(s)];
const svg={
 computer:'<svg viewBox="0 0 24 24" aria-hidden="true"><rect x="3" y="4" width="18" height="12" rx="2"/><path d="M8 20h8M12 16v4"/></svg>',
 console:'<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M7.2 8h9.6c2.2 0 3.9 1.6 4.2 3.7l.7 4.6c.3 2-1.9 3.4-3.5 2.2l-2.4-1.8H8.2l-2.4 1.8c-1.6 1.2-3.8-.2-3.5-2.2l.7-4.6C3.3 9.6 5 8 7.2 8Z"/><path d="M7 11v4M5 13h4M16.5 12.2h.01M18.5 14h.01"/></svg>',
 laptop:'<svg viewBox="0 0 24 24" aria-hidden="true"><rect x="5" y="4" width="14" height="11" rx="2"/><path d="M3 19h18M8 19l1-2h6l1 2"/></svg>',
 desktop:'<svg viewBox="0 0 24 24" aria-hidden="true"><rect x="3" y="4" width="18" height="12" rx="2"/><path d="M9 20h6M12 16v4"/></svg>',
 gp:'<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 2v20M17 6.2c-1.1-1-2.7-1.7-4.6-1.7-2.8 0-4.9 1.4-4.9 3.5 0 5.3 9.5 2.4 9.5 7.5 0 2.3-2.1 4-5.2 4-2.2 0-4.1-.8-5.3-2"/></svg>'
};
function addStyles(){if($('#morleyVideoPolishStyles'))return;const s=document.createElement('style');s.id='morleyVideoPolishStyles';s.textContent=`
@media(max-width:760px){
  html,body{overflow-x:hidden!important}
  main.app,.app{padding-bottom:calc(128px + env(safe-area-inset-bottom))!important}
  .section{scroll-margin-top:12px!important;padding-bottom:18px!important}
  .top{position:relative!important;z-index:3!important;overflow:visible!important}
  .morley-menu-trigger-fixed,.morley-web-user{position:absolute!important;z-index:4!important}
  body nav{z-index:40!important}
  #home .tiles button{position:relative!important;overflow:hidden!important;min-height:108px!important;display:flex!important;flex-direction:column!important;align-items:flex-start!important;justify-content:center!important;gap:7px!important;padding:16px 18px!important}
  #home .tiles button small{color:rgba(255,255,255,.9)!important;opacity:1!important;line-height:1.35!important}
  #home .tiles button b{color:#fff!important}
  #home .tiles button::before,#home .tiles button::after,.msw-metric::before,.msw-metric::after,.msw-recent::before,.msw-recent::after,.card::before,.card::after{content:none!important;display:none!important}
  .morley-home-tile-icon,.morley-choice-icon{display:grid!important;place-items:center!important;width:28px!important;height:28px!important;color:#eafff8!important;flex:0 0 auto!important}
  .morley-home-tile-icon svg,.morley-choice-icon svg{width:28px!important;height:28px!important;display:block!important;fill:none!important;stroke:currentColor!important;stroke-width:1.9!important;stroke-linecap:round!important;stroke-linejoin:round!important}
  .parity-choice{min-height:122px!important;gap:7px!important;padding:16px!important}
  .parity-choice>span:not(.morley-choice-icon){display:none!important}
  .parity-choice small{color:#c8d4d0!important}
  .morley-recent-head{display:flex!important;align-items:center!important;justify-content:space-between!important;gap:12px!important;flex-wrap:nowrap!important}
  .morley-recent-title{font-size:20px!important;line-height:1.2!important;margin:0!important;min-width:0!important}
  .morley-clear-button{display:inline-flex!important;align-items:center!important;justify-content:center!important;width:auto!important;min-width:76px!important;max-width:none!important;min-height:40px!important;height:40px!important;padding:0 14px!important;white-space:nowrap!important;word-break:normal!important;overflow-wrap:normal!important;writing-mode:horizontal-tb!important;line-height:1!important;font-size:13px!important;flex:0 0 auto!important}
  .morley-menu-trigger-fixed,.morley-web-user{transform:none!important}
}
@media(max-width:390px){
  main.app,.app{padding-bottom:calc(132px + env(safe-area-inset-bottom))!important}
  .morley-recent-title{font-size:18px!important}
  .morley-clear-button{min-width:68px!important;padding:0 12px!important}
}
`;document.head.appendChild(s);}
function replaceLeadingGraphic(button,kind){if(!button||!svg[kind])return;[...button.childNodes].forEach(n=>{if(n.nodeType===3&&n.textContent.trim())n.remove();});let icon=$('.morley-home-tile-icon',button);if(!icon){icon=document.createElement('span');icon.className='morley-home-tile-icon';button.prepend(icon);}icon.innerHTML=svg[kind];}
function polishHomeTiles(){$$('#home .tiles button').forEach(b=>{const label=(b.querySelector('b')?.textContent||b.textContent).toLowerCase();if(label.includes('computer'))replaceLeadingGraphic(b,'computer');else if(label.includes('console'))replaceLeadingGraphic(b,'console');else if(label.includes('general')||label.includes('gp'))replaceLeadingGraphic(b,'gp');});}
function polishComputerChoices(){const map=[['#parityLaptop','laptop'],['#parityDesktop','desktop']];map.forEach(([sel,kind])=>{const b=$(sel);if(!b)return;const old=b.querySelector(':scope>span:not(.morley-choice-icon)');if(old)old.remove();let icon=$('.morley-choice-icon',b);if(!icon){icon=document.createElement('span');icon.className='morley-choice-icon';b.prepend(icon);}icon.innerHTML=svg[kind];});}
function polishRecent(){const clear=$$('button').find(b=>b.textContent.trim().toLowerCase()==='clear');if(clear){clear.classList.add('morley-clear-button');const p=clear.parentElement;if(p)p.classList.add('morley-recent-head');const heading=p?.querySelector('h1,h2,h3,strong');if(heading){heading.classList.add('morley-recent-title');heading.textContent=heading.textContent.replace(/^\s*[🕒⏱️⏰]\s*/u,'');}}
  $$('h1,h2,h3').filter(h=>/recent valuations/i.test(h.textContent)).forEach(h=>h.classList.add('morley-recent-title'));
}
function apply(){addStyles();polishHomeTiles();polishComputerChoices();polishRecent();}
let queued=false;function schedule(){if(queued)return;queued=true;(requestAnimationFrame||setTimeout)(()=>{queued=false;apply();});}
function boot(){apply();const o=new MutationObserver(schedule);o.observe(document.body,{subtree:true,childList:true,attributes:true,attributeFilter:['class','hidden']});addEventListener('resize',schedule,{passive:true});addEventListener('morley-product-parity-ready',schedule);}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot,{once:true});else boot();
})();
