(()=>{'use strict';
const MAX_WIDTH=960;
let raf=0;
function visiblePanel(){return document.querySelector('#appView>.panel:not(.hidden)')}
function schedule(){if(raf)return;raf=requestAnimationFrame(()=>{raf=0;update()})}
function update(){
  const body=document.body,tabs=document.querySelector('.tabs'),panel=visiblePanel();
  if(!body||!tabs||!panel||innerWidth>MAX_WIDTH||!body.classList.contains('admin-v2')){
    body?.classList.remove('admin-nav-content-docked');
    document.documentElement.style.removeProperty('--admin-nav-dock-top');
    return;
  }
  const rect=panel.getBoundingClientRect();
  const navHeight=Math.max(62,tabs.getBoundingClientRect().height||0);
  const reserve=navHeight+28;
  const available=innerHeight-reserve;
  const shouldDock=rect.bottom>72&&rect.bottom<available-56;
  body.classList.toggle('admin-nav-content-docked',shouldDock);
  if(shouldDock){
    const top=Math.max(76,Math.round(rect.bottom+12));
    document.documentElement.style.setProperty('--admin-nav-dock-top',`${top}px`);
  }else document.documentElement.style.removeProperty('--admin-nav-dock-top');
}
addEventListener('resize',schedule,{passive:true});
addEventListener('orientationchange',schedule,{passive:true});
addEventListener('scroll',schedule,{passive:true});
document.addEventListener('click',event=>{if(event.target.closest('.tabs .tab,.tabs a'))setTimeout(schedule,0)});
const root=document.querySelector('#appView')||document.body;
new MutationObserver(schedule).observe(root,{subtree:true,childList:true,attributes:true,attributeFilter:['class']});
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',schedule,{once:true});else schedule();
setTimeout(schedule,250);setTimeout(schedule,1000);
})();
