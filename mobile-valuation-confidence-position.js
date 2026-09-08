(()=>{
'use strict';
const mq=window.matchMedia('(max-width:760px)');
function place(){
  const panel=document.getElementById('morleySmartInsights');
  const home=document.getElementById('home');
  const workspace=document.getElementById('morleySmartWorkspace');
  if(!panel||!home||!workspace)return;
  if(mq.matches){
    if(panel.parentElement!==home||panel!==home.lastElementChild)home.appendChild(panel);
  }else if(panel.parentElement!==workspace){
    workspace.appendChild(panel);
  }
}
function schedule(){requestAnimationFrame(place)}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',schedule,{once:true});else schedule();
new MutationObserver(schedule).observe(document.documentElement,{childList:true,subtree:true});
if(mq.addEventListener)mq.addEventListener('change',schedule);else mq.addListener(schedule);
window.addEventListener('orientationchange',schedule);
})();
