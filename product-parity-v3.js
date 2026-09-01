(()=>{
'use strict';
const $=(s,r=document)=>r.querySelector(s), $$=(s,r=document)=>[...r.querySelectorAll(s)];
let activeObserver=null;

function go(page){
  if(typeof window.morleyDesktopGo==='function') window.morleyDesktopGo(page);
  else if(typeof window.show==='function') window.show(page);
}

function removeLegacyParitySections(){
  $('#computer')?.remove();
  $('#console')?.remove();
}

function homeParity(){
  const tiles=$('#home .tiles');
  if(!tiles) return;

  const all=[...tiles.children];
  const laptop=all.find(el=>/computer pricing|laptop\s*\/\s*macbook/i.test(el.textContent||'')) || $('[onclick="show(\'laptop\')"]',tiles);
  const desktop=all.find(el=>/console pricing|desktop\s*\/\s*gaming pc/i.test(el.textContent||'')) || $('[onclick="show(\'desktop\')"]',tiles);
  const gp=all.find(el=>/general buys|\bgp\b/i.test(el.textContent||''));

  if(laptop){
    laptop.setAttribute('onclick',"show('laptop')");
    laptop.innerHTML='<span class="parity-icon" aria-hidden="true">▱</span><b>Laptop / MacBook</b><small>Guided exact-model laptop and MacBook valuation</small>';
  }
  if(desktop){
    desktop.setAttribute('onclick',"show('desktop')");
    desktop.innerHTML='<span class="parity-icon" aria-hidden="true">▦</span><b>Desktop / Gaming PC</b><small>Desktop and gaming PC component-based valuation</small>';
  }
  if(gp){
    gp.setAttribute('onclick',"show('general')");
    gp.innerHTML='<span class="parity-icon" aria-hidden="true">$</span><b>General Buys / GP</b><small>A / B / C / Luxury buying targets</small>';
  }
}

function navParity(){
  const nav=$('nav');
  if(nav){
    const signature='home|laptop|desktop|general';
    if(nav.dataset.morleyParity!==signature){
      nav.innerHTML=`<button data-page="home" class="active" type="button"><span class="nav-symbol">⌂</span><small>Home</small></button><button data-page="laptop" type="button"><span class="nav-symbol">▱</span><small>Laptop</small></button><button data-page="desktop" type="button"><span class="nav-symbol">▦</span><small>Desktop</small></button><button data-page="general" type="button"><span class="nav-symbol">$</span><small>GP</small></button>`;
      nav.dataset.morleyParity=signature;
      $$('button[data-page]',nav).forEach(b=>b.onclick=()=>go(b.dataset.page));
      nav.style.gridTemplateColumns='repeat(4,1fr)';
    }
  }

  $$('.desktop-side-nav [data-target]').forEach(b=>{
    const text=(b.textContent||'').trim();
    if(b.dataset.target==='computer' || /computer pricing/i.test(text)){
      b.dataset.target='laptop';
      b.innerHTML=b.innerHTML.replace(/Computer Pricing/i,'Laptop / MacBook');
    } else if(b.dataset.target==='console' || /console pricing/i.test(text)){
      b.dataset.target='desktop';
      b.innerHTML=b.innerHTML.replace(/Console Pricing/i,'Desktop / Gaming PC');
    }
  });
}

function syncActiveNav(){
  const active=$('.section.active')?.id||'home';
  $$('nav [data-page]').forEach(b=>b.classList.toggle('active',b.dataset.page===active));
}

function activeNavParity(){
  if(activeObserver) return;
  const main=$('main.app')||$('main');
  if(!main) return;
  let queued=false;
  activeObserver=new MutationObserver(()=>{
    if(queued) return;
    queued=true;
    requestAnimationFrame(()=>{queued=false;syncActiveNav();});
  });
  activeObserver.observe(main,{subtree:true,attributes:true,attributeFilter:['class']});
  syncActiveNav();
}

function terminologyParity(){
  const gp=$('#general h2'), lap=$('#laptop h2'), desk=$('#desktop h2');
  if(gp) gp.textContent='General Buys / GP';
  if(lap) lap.textContent='Laptop / MacBook';
  if(desk) desk.textContent='Desktop / Gaming PC';
}

function addStyles(){
  if($('#productParityStyles')) return;
  const s=document.createElement('style');
  s.id='productParityStyles';
  s.textContent=`
    .parity-icon,.nav-symbol{display:inline-grid;place-items:center;color:#77e9c4;font-weight:900;line-height:1}
    .parity-icon{font-size:24px;margin-bottom:3px}
    nav .nav-symbol{font-size:20px;min-height:22px}
    #home .tiles small{color:#d1dcda!important;opacity:.86}
  `;
  document.head.appendChild(s);
}

let booted=false;
function boot(){
  removeLegacyParitySections();
  homeParity();
  navParity();
  activeNavParity();
  terminologyParity();
  addStyles();
  booted=true;
  window.dispatchEvent(new CustomEvent('morley-product-parity-ready'));
}
function scheduleBoot(){
  if(booted){removeLegacyParitySections();homeParity();navParity();terminologyParity();syncActiveNav();return;}
  setTimeout(boot,0);
}
if(document.readyState==='loading') document.addEventListener('DOMContentLoaded',scheduleBoot,{once:true});
else scheduleBoot();
window.addEventListener('load',()=>{if(!booted)boot();},{once:true});
})();
