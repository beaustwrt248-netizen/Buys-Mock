(()=>{
'use strict';
const $=(s,r=document)=>r.querySelector(s), $$=(s,r=document)=>[...r.querySelectorAll(s)];
const isMobile=()=>window.matchMedia('(max-width:760px)').matches;
let observer=null,queued=false,writingNav=false;

function go(page){
  if(typeof window.morleyDesktopGo==='function') window.morleyDesktopGo(page);
  else if(typeof window.show==='function') window.show(page);
}

const icons={
  home:'<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M3 10.8 12 3l9 7.8v9.7a.5.5 0 0 1-.5.5h-5.7v-6.2H9.2V21H3.5a.5.5 0 0 1-.5-.5v-9.7Z"/></svg>',
  categories:'<svg viewBox="0 0 24 24" aria-hidden="true"><rect x="3" y="3" width="7" height="7" rx="1.5"/><rect x="14" y="3" width="7" height="7" rx="1.5"/><rect x="3" y="14" width="7" height="7" rx="1.5"/><rect x="14" y="14" width="7" height="7" rx="1.5"/></svg>',
  general:'<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 2v20M17 6.2c-1.1-1-2.7-1.7-4.6-1.7-2.8 0-4.9 1.4-4.9 3.5 0 5.3 9.5 2.4 9.5 7.5 0 2.3-2.1 4-5.2 4-2.2 0-4.1-.8-5.3-2"/></svg>',
  more:'<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="5" cy="12" r="1.4"/><circle cx="12" cy="12" r="1.4"/><circle cx="19" cy="12" r="1.4"/></svg>'
};

function navButton(page,label,iconKey=page){
  return `<button data-page="${page}" type="button" aria-label="${label}"><span class="morley-nav-icon">${icons[iconKey]}</span><small>${label}</small></button>`;
}

function ensureStyles(){
  let style=$('#morleyMobileLayoutFinal');
  if(style)return;
  style=document.createElement('style');
  style.id='morleyMobileLayoutFinal';
  style.textContent=`
@media(max-width:760px){
  main.app,.app{padding-bottom:calc(86px + env(safe-area-inset-bottom))!important}
  .top{min-height:58px!important;padding:8px 136px 8px 104px!important;display:flex!important;align-items:center!important;gap:8px!important;box-sizing:border-box!important}
  .top>b{font-size:17px!important;line-height:1.15!important;white-space:nowrap!important;overflow:hidden!important;text-overflow:ellipsis!important;margin:0!important;color:#f4f7f6!important;text-shadow:0 1px 1px rgba(0,0,0,.22)!important}
  .morley-menu-trigger-fixed{left:12px!important;top:9px!important;width:88px!important;min-width:88px!important;max-width:88px!important;height:40px!important;min-height:40px!important;padding:0 9px!important;border-radius:13px!important;display:flex!important;align-items:center!important;justify-content:center!important;gap:6px!important;box-sizing:border-box!important}
  .morley-menu-trigger-label{font-size:11px!important;line-height:1!important}
  .morley-web-user{top:9px!important;right:12px!important;width:120px!important;max-width:120px!important;min-height:40px!important;height:40px!important;padding:0 9px!important;font-size:11px!important;border-radius:999px!important;box-sizing:border-box!important}
  body nav{left:12px!important;right:12px!important;bottom:9px!important;width:auto!important;height:auto!important;min-height:0!important;max-height:74px!important;display:grid!important;grid-template-columns:repeat(4,minmax(0,1fr))!important;gap:5px!important;padding:5px 5px calc(5px + env(safe-area-inset-bottom))!important;border:1px solid rgba(56,214,163,.22)!important;border-radius:18px!important;background:rgba(8,11,13,.97)!important;box-shadow:0 10px 28px rgba(0,0,0,.36)!important;box-sizing:border-box!important;overflow:hidden!important}
  body nav button,body nav button.active{position:relative!important;display:flex!important;flex-direction:column!important;align-items:center!important;justify-content:center!important;gap:3px!important;width:100%!important;min-width:0!important;height:52px!important;min-height:52px!important;max-height:52px!important;margin:0!important;padding:5px 3px!important;border-radius:12px!important;border:1px solid transparent!important;background:transparent!important;background-image:none!important;box-shadow:none!important;transform:none!important;color:#a9b9b4!important;font-size:0!important;line-height:1!important;box-sizing:border-box!important;overflow:hidden!important}
  body nav button.active{background:rgba(56,214,163,.13)!important;border-color:rgba(56,214,163,.3)!important;color:#77e9c4!important}
  body nav button::before,body nav button::after{content:none!important;display:none!important;background:none!important}
  body nav .morley-nav-icon{display:grid!important;place-items:center!important;width:22px!important;height:22px!important;min-width:22px!important;min-height:22px!important;margin:0!important;padding:0!important;color:inherit!important}
  body nav .morley-nav-icon svg{display:block!important;width:22px!important;height:22px!important;fill:none!important;stroke:currentColor!important;stroke-width:1.9!important;stroke-linecap:round!important;stroke-linejoin:round!important;overflow:visible!important}
  body nav button small{display:block!important;margin:0!important;padding:0!important;font-size:10px!important;font-weight:700!important;line-height:12px!important;color:inherit!important;white-space:nowrap!important}
  body nav button[data-page="desktop"]{display:none!important}
}
@media(max-width:390px){
  .top{padding-left:98px!important;padding-right:116px!important}
  .morley-menu-trigger-fixed{width:82px!important;min-width:82px!important;max-width:82px!important}
  .morley-web-user{width:104px!important;max-width:104px!important}
  .top>b{font-size:15px!important}
  body nav{left:8px!important;right:8px!important;gap:4px!important;padding-left:4px!important;padding-right:4px!important}
  body nav button,body nav button.active{height:50px!important;min-height:50px!important;max-height:50px!important}
  body nav button small{font-size:9px!important}
}
`;
  document.head.appendChild(style);
}

function parityV5OwnsMobile(){
  return isMobile()&&(document.documentElement.dataset.morleyMobileAppParity==='5'||!!document.querySelector('link[data-morley-mobile-app-parity="5"]'));
}
function parityV4OwnsNav(){
  return parityV5OwnsMobile()||document.documentElement.dataset.morleyMobileAppParity==='4'||!!document.querySelector('link[data-morley-mobile-app-parity="4"]');
}

function mobileNav(){
  if(!isMobile()||parityV4OwnsNav()) return;
  const nav=$('nav');
  if(!nav) return;
  const signature='home|laptop|general|settings';
  const current=$$('button[data-page]',nav).map(b=>b.dataset.page).join('|');
  const needsCleanMarkup=current!==signature||nav.dataset.morleyMobileNav!=='v5'||!nav.querySelector('.morley-nav-icon');
  if(needsCleanMarkup&&!writingNav){
    writingNav=true;
    nav.innerHTML=navButton('home','Home')+navButton('laptop','Categories','categories')+navButton('general','General Buys','general')+navButton('settings','More','more');
    $$('button[data-page]',nav).forEach(b=>b.addEventListener('click',()=>go(b.dataset.page)));
    nav.dataset.morleyMobileNav='v5';
    writingNav=false;
  }
  const active=$('.section.active')?.id||'home';
  const mapped=(active==='laptop'||active==='desktop'||active==='computer'||active==='console')?'laptop':(active==='more'?'settings':active);
  $$('button[data-page]',nav).forEach(b=>b.classList.toggle('active',b.dataset.page===mapped));
}

function overlayState(){
  const nav=$('nav');
  if(!isMobile()){
    document.body.classList.remove('morley-mobile-overlay-active');
    nav?.style.removeProperty('display');
    return;
  }
  const moreActive=!!$('#more.section.active #morleyRelevantMore, #settings.section.active #morleyRelevantMore');
  const dialogOpen=!!$('.morley-menu-dialog.open,.msw-dialog.open,.modal.open,[role="dialog"].open');
  const hidden=moreActive||dialogOpen;
  document.body.classList.toggle('morley-mobile-overlay-active',hidden);
  if(nav){
    if(hidden) nav.style.setProperty('display','none','important');
    else nav.style.removeProperty('display');
  }
}

function fixMarginInsight(){
  $$('.mswp-insight>div').forEach(block=>{
    const label=block.querySelector('small'),value=block.querySelector('b');
    if(label&&value){label.style.display='block';value.style.display='block';}
  });
}

function apply(){
  if(parityV5OwnsMobile()){
    $('#morleyMobileLayoutFinal')?.remove();
    document.body.classList.remove('morley-mobile-overlay-active');
    $('nav')?.style.removeProperty('display');
    fixMarginInsight();
    return;
  }
  ensureStyles();
  mobileNav();
  overlayState();
  fixMarginInsight();
}
function schedule(){
  if(queued)return;
  queued=true;
  (window.requestAnimationFrame||window.setTimeout)(()=>{queued=false;apply();});
}
function boot(){
  apply();
  if(observer)return;
  observer=new MutationObserver(schedule);
  observer.observe(document.body,{subtree:true,childList:true,attributes:true,attributeFilter:['class','hidden','data-page']});
  window.addEventListener('resize',schedule,{passive:true});
  window.addEventListener('morley-product-parity-ready',schedule);
}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot,{once:true});else boot();
})();
