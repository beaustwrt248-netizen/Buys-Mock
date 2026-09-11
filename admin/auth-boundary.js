(()=>{'use strict';
const root=document.documentElement;
const app=()=>document.getElementById('appView');
const login=()=>document.getElementById('loginView');

function authenticated(){
  const view=app();
  return !!view&&!view.classList.contains('hidden')&&!!login()?.classList.contains('hidden');
}
function syncState(){
  const ok=authenticated();
  root.classList.toggle('admin-authenticated',ok);
  root.classList.toggle('admin-logged-out',!ok);
  const menu=document.getElementById('adminMoreMenu');
  const view=app();
  if(menu&&view&&menu.parentElement!==view)view.appendChild(menu);
  if(menu)menu.dataset.adminPrivilegedNav='true';
  if(!ok&&menu)menu.classList.remove('open');
}
function boot(){
  syncState();
  const observer=new MutationObserver(syncState);
  observer.observe(document.body,{subtree:true,childList:true,attributes:true,attributeFilter:['class']});
  addEventListener('pageshow',syncState);
  document.addEventListener('visibilitychange',()=>{if(!document.hidden)syncState()});
}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot,{once:true});else boot();
})();
