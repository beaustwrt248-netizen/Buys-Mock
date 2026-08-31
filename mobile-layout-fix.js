(()=>{
'use strict';
const $=(s,r=document)=>r.querySelector(s), $$=(s,r=document)=>[...r.querySelectorAll(s)];
const isMobile=()=>window.matchMedia('(max-width:760px)').matches;
let observer=null,queued=false,writingNav=false;

function go(page){
  if(typeof window.morleyDesktopGo==='function') window.morleyDesktopGo(page);
  else if(typeof window.show==='function') window.show(page);
}

function mobileNav(){
  if(!isMobile()) return;
  const nav=$('nav');
  if(!nav) return;
  const signature='home|computer|console|general';
  const current=$$('button[data-page]',nav).map(b=>b.dataset.page).join('|');
  if(current!==signature && !writingNav){
    writingNav=true;
    nav.innerHTML='<button data-page="home" class="active" type="button">⌂<small>Home</small></button><button data-page="computer" type="button">▱<small>Computer</small></button><button data-page="console" type="button">◫<small>Console</small></button><button data-page="general" type="button">$<small>GP</small></button>';
    $$('button[data-page]',nav).forEach(b=>b.addEventListener('click',()=>go(b.dataset.page)));
    nav.dataset.morleyMobileNav='v3';
    writingNav=false;
  }
  const active=$('.section.active')?.id||'home';
  const mapped=(active==='laptop'||active==='desktop')?'computer':active;
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
    if(label&&value){ label.style.display='block'; value.style.display='block'; }
  });
}

function apply(){
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
