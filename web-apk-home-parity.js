(()=>{
'use strict';
const HOME_ID='home';
const HIDDEN='data-apk-parity-hidden';
let observer=null;
let scheduled=false;

function route(name){
  if(typeof window.show==='function'){ window.show(name); return; }
  const btn=document.querySelector(`[data-page="${name}"]`);
  if(btn instanceof HTMLElement) btn.click();
}

function launch(label,icon,sub,target){
  const b=document.createElement('button');
  b.type='button';
  b.className='apk-launch';
  const iconEl=document.createElement('span');
  iconEl.className='apk-icon';
  iconEl.textContent=icon;
  const copy=document.createElement('span');
  copy.textContent=label;
  const small=document.createElement('small');
  small.textContent=sub;
  copy.appendChild(small);
  b.append(iconEl,copy);
  b.addEventListener('click',()=>route(target));
  return b;
}

function card(label,copy){
  const el=document.createElement('div');
  el.className='apk-card';
  const labelEl=document.createElement('div');
  labelEl.className='apk-card-label';
  labelEl.textContent=label;
  const copyEl=document.createElement('div');
  copyEl.className='apk-card-copy';
  copyEl.textContent=copy;
  el.append(labelEl,copyEl);
  return el;
}

function buildShell(){
  const shell=document.createElement('div');
  shell.className='apk-home-parity';
  shell.setAttribute('data-apk-parity-owned','true');

  const title=document.createElement('h1');
  title.className='apk-title';
  title.textContent='Native valuation workspace';
  shell.append(title);
  shell.append(card('WELCOME','Live valuation and buying targets in the same clean workflow as the Morley Android app.'));
  shell.append(launch('Laptop / MacBook','💻','Exact configuration market valuation','laptop'));
  shell.append(launch('Desktop / Gaming PC','🖥️','Component and whole-PC pricing','desktop'));
  shell.append(launch('GP Calculator','$','A/B/C/Luxury buying targets','general'));

  const online=document.createElement('div');
  online.className='apk-online';
  online.innerHTML='<div class="apk-mini"><small>LIVE PRICING</small><b>READY</b></div><div class="apk-mini"><small>ONLINE STATUS</small><b>ONLINE</b></div>';
  shell.append(online);
  shell.append(card('ONLINE PRICING','Direct live-pricing access with the same protected valuation rules used by Morley.'));
  return shell;
}

function enforce(){
  const home=document.getElementById(HOME_ID);
  if(!home) return;

  let shell=home.querySelector(':scope > .apk-home-parity');
  if(!shell){
    shell=buildShell();
    home.prepend(shell);
  }

  home.dataset.apkHomeParity='true';
  for(const child of Array.from(home.children)){
    if(child===shell || child.classList.contains('apk-home-parity')) continue;
    child.setAttribute(HIDDEN,'true');
    child.setAttribute('aria-hidden','true');
    child.style.setProperty('display','none','important');
  }
}

function schedule(){
  if(scheduled) return;
  scheduled=true;
  (window.requestAnimationFrame||window.setTimeout)(()=>{
    scheduled=false;
    enforce();
  });
}

function boot(){
  enforce();
  observer=new MutationObserver(schedule);
  observer.observe(document.body,{childList:true,subtree:true});
  window.addEventListener('load',enforce,{once:true});
  window.addEventListener('morley-initial-layout-ready',enforce);
  window.addEventListener('morley-product-parity-ready',enforce);
  setTimeout(enforce,120);
  setTimeout(enforce,500);
  setTimeout(enforce,1200);
}

if(document.readyState==='loading') document.addEventListener('DOMContentLoaded',boot,{once:true});
else boot();
})();
