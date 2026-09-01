(()=>{
'use strict';
const $=(s,r=document)=>r.querySelector(s),$$=(s,r=document)=>[...r.querySelectorAll(s)];
const ROUTES=new Set(['home','computer','console','general']);
let finished=false;

function explicitRoute(){
  const hash=(location.hash||'').replace(/^#/,'').trim();
  return ROUTES.has(hash)?hash:null;
}

function currentSection(){
  return $('.section.active');
}

function activateDirect(page){
  const target=$(`#${page}.section`);
  if(!target)return false;
  $$('.section').forEach(section=>section.classList.toggle('active',section===target));
  $$('nav [data-page]').forEach(button=>button.classList.toggle('active',button.dataset.page===page));
  return true;
}

function go(page){
  if(typeof window.morleyDesktopGo==='function'){
    try{if(window.morleyDesktopGo(page,{replace:true})!==false)return true}catch(_){ }
  }
  if(typeof window.show==='function'){
    try{window.show(page);return true}catch(_){ }
  }
  return activateDirect(page);
}

function needsRepair(){
  const active=currentSection();
  if(!active)return true;
  const style=getComputedStyle(active);
  return style.display==='none'||style.visibility==='hidden';
}

function refreshLayout(){
  try{window.dispatchEvent(new Event('resize'))}catch(_){ }
  try{window.dispatchEvent(new CustomEvent('morley-initial-layout-ready'))}catch(_){ }
}

function sync(){
  if(finished)return;
  const requested=explicitRoute();
  const active=currentSection();
  if(needsRepair())go(requested||active?.id||'home');
  else if(requested&&active?.id!==requested)go(requested);
  refreshLayout();
}

function boot(){
  sync();
  requestAnimationFrame(()=>{sync();requestAnimationFrame(sync)});
  [80,220,500,900].forEach(delay=>setTimeout(sync,delay));
  setTimeout(()=>{sync();finished=true},1300);
}

if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot,{once:true});
else boot();
window.addEventListener('load',sync,{once:true});
})();
