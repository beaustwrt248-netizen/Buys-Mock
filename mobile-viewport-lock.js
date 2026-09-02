(()=>{
'use strict';
const ROOT=document.documentElement;
const CLASS='morley-physical-phone';
const LOCK_HREF='mobile-viewport-lock.css?v=20260902-1';

function physicalPhone(){
  const sw=Number(screen?.width)||Infinity;
  const sh=Number(screen?.height)||Infinity;
  const physicalMin=Math.min(sw,sh);
  const vv=window.visualViewport;
  const visualMin=Math.min(Number(vv?.width)||Infinity,Number(vv?.height)||Infinity);
  return physicalMin<=600||visualMin<=600;
}

function syncClass(){
  ROOT.classList.toggle(CLASS,physicalPhone());
  ROOT.dataset.morleyViewportMode=physicalPhone()?'phone':'responsive';
}

function ensureFinalLock(){
  let link=document.querySelector('link[data-morley-viewport-lock]');
  if(!link){
    link=document.createElement('link');
    link.rel='stylesheet';
    link.href=LOCK_HREF;
    link.dataset.morleyViewportLock='1';
  }
  // Moving the link to the end makes this the authoritative layer even after
  // legacy scripts append runtime <style> blocks during startup.
  document.head.appendChild(link);
}

function sync(){
  syncClass();
  ensureFinalLock();
}

syncClass();
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',sync,{once:true});else sync();
window.addEventListener('resize',sync,{passive:true});
window.addEventListener('orientationchange',sync,{passive:true});
window.visualViewport?.addEventListener('resize',sync,{passive:true});
window.addEventListener('morley-initial-layout-ready',sync);
window.addEventListener('morley-product-parity-ready',sync);
})();
