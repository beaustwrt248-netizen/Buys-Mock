(()=>{'use strict';
const q=(s,r=document)=>r.querySelector(s),qa=(s,r=document)=>[...r.querySelectorAll(s)];
const mobile=()=>window.matchMedia('(max-width:960px)').matches;

function activeTab(){return q('.tabs .tab.active[data-tab]')}
function enforceSingleWorkspace(){
  const active=activeTab();
  if(!active)return;
  const id='tab-'+active.dataset.tab;
  qa('#appView>.panel').forEach(panel=>panel.classList.toggle('hidden',panel.id!==id));
}
function moveToWorkspaceTop(){
  if(!mobile())return;
  const top=q('.topbar')?.getBoundingClientRect().bottom||0;
  const y=Math.max(0,window.scrollY+((q('#appView>.panel:not(.hidden)')?.getBoundingClientRect().top||top)-top)-6);
  window.scrollTo({top:y,behavior:'auto'});
}
function bindNavigation(){
  qa('.tabs .tab[data-tab]').forEach(btn=>{
    if(btn.dataset.mobileWorkspaceBound)return;
    btn.dataset.mobileWorkspaceBound='1';
    btn.addEventListener('click',()=>requestAnimationFrame(()=>{enforceSingleWorkspace();moveToWorkspaceTop()}));
  });
}

function enhanceUserRows(){
  const list=q('#usersList');if(!list)return;
  qa(':scope > .row',list).forEach(row=>{
    if(q('.admin-mobile-user-toggle',row))return;
    const button=document.createElement('button');
    button.type='button';button.className='admin-mobile-user-toggle';button.textContent='Manage';button.setAttribute('aria-expanded','false');
    button.addEventListener('click',()=>{const expanded=row.classList.toggle('mobile-expanded');button.textContent=expanded?'Close':'Manage';button.setAttribute('aria-expanded',String(expanded))});
    row.appendChild(button);
  });
}

function labelBefore(el){const p=el?.previousElementSibling;return p&&p.tagName==='LABEL'?p:null}
function enhanceReleaseIntegrity(){
  const card=q('#tab-release>.card');if(!card||q('.admin-release-integrity',card))return;
  const sha=q('#releaseSha',card),url=q('#releaseUrl',card),notes=q('#releaseNotes',card);if(!sha||!url||!notes)return;
  const details=document.createElement('details');details.className='admin-release-integrity';
  const summary=document.createElement('summary');summary.textContent='Release integrity details';details.appendChild(summary);
  for(const field of [sha,url,notes]){const label=labelBefore(field);if(label)details.appendChild(label);details.appendChild(field)}
  const anchor=q('#releaseNotes',details)||notes;card.insertBefore(details,anchor.nextElementSibling);
  const sync=()=>{details.open=!mobile()};sync();window.matchMedia('(max-width:960px)').addEventListener?.('change',sync);
}

function observeDynamicLists(){
  const users=q('#usersList');if(users&&!users.dataset.mobileWorkspaceObserved){users.dataset.mobileWorkspaceObserved='1';new MutationObserver(()=>enhanceUserRows()).observe(users,{childList:true});enhanceUserRows()}
}
function run(){bindNavigation();enforceSingleWorkspace();observeDynamicLists();enhanceReleaseIntegrity();document.documentElement.dataset.adminMobileWorkspace='ready'}
function boot(){run();new MutationObserver(()=>requestAnimationFrame(run)).observe(q('#appView')||document.body,{subtree:true,childList:true,attributes:true,attributeFilter:['class']})}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot,{once:true});else boot();
})();
