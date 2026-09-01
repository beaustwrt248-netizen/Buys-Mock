(()=>{
'use strict';
const $=(s,r=document)=>r.querySelector(s), $$=(s,r=document)=>[...r.querySelectorAll(s)];
const PRODUCT_ROUTES=new Set(['computer','console']);
const ROUTE_KEY='morley_desktop_route';

function activateProductRoute(page,opts={}){
  if(!PRODUCT_ROUTES.has(page)) return false;
  const target=$(`#${page}.section`);
  if(!target) return false;
  $$('.section').forEach(section=>section.classList.toggle('active',section===target));
  $$('nav [data-page]').forEach(button=>button.classList.toggle('active',button.dataset.page===page));
  $$('.desktop-side-nav [data-target]').forEach(button=>button.classList.toggle('active',button.dataset.target===page));
  try{localStorage.setItem(ROUTE_KEY,page)}catch{}
  const hash=`#${page}`;
  if(location.hash!==hash){
    if(opts.replace) history.replaceState({morleyPage:page},'',hash);
    else history.pushState({morleyPage:page},'',hash);
  }
  window.scrollTo({top:0,behavior:'smooth'});
  window.dispatchEvent(new CustomEvent('morley-product-route',{detail:{page}}));
  return true;
}

function install(){
  if(window.__morleyProductRouteFixInstalled) return;
  const original=window.morleyDesktopGo;
  window.morleyDesktopGo=function(page,opts={}){
    if(activateProductRoute(page,opts)) return true;
    return typeof original==='function' ? original(page,opts) : false;
  };
  window.__morleyProductRouteFixInstalled=true;

  addEventListener('popstate',()=>{
    const page=(location.hash||'').replace('#','');
    if(PRODUCT_ROUTES.has(page)) activateProductRoute(page,{replace:true});
  });

  const initial=(location.hash||'').replace('#','');
  if(PRODUCT_ROUTES.has(initial)) setTimeout(()=>activateProductRoute(initial,{replace:true}),0);
}

if(document.readyState==='loading') document.addEventListener('DOMContentLoaded',install,{once:true});
else install();
})();
