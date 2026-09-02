(()=>{'use strict';
const $=(s,r=document)=>r.querySelector(s),$$=(s,r=document)=>[...r.querySelectorAll(s)];
const navItems=[
 ['home','⌂','Dashboard'],['computer','▱','Computer Pricing'],['console','◫','Console Pricing'],
 ['general','$','General Buys'],['deals','◷','Saved Deals'],['inventory','▣','Inventory'],
 ['scanner','⌗','Scanner'],['sales','↗','Sales'],['settings','•••','More']
];
function go(page){if(typeof window.morleyDesktopGo==='function')window.morleyDesktopGo(page);else if(typeof window.show==='function')window.show(page);}
function renderSidebar(){
 const nav=$('#morleyDesktopShell .desktop-side-nav');if(!nav)return;
 if(nav.dataset.financeDashboard==='1')return;
 nav.innerHTML=navItems.map(([target,icon,label])=>`<button type="button" data-target="${target}" aria-label="${label}"><span class="morley-dashboard-nav-icon" aria-hidden="true">${icon}</span><span class="morley-dashboard-nav-label">${label}</span></button>`).join('');
 nav.dataset.financeDashboard='1';
 $$('button[data-target]',nav).forEach(button=>button.addEventListener('click',()=>go(button.dataset.target)));
 syncActive();
}
function syncActive(){
 const active=$('.section.active')?.id||'home';
 const parent=['laptop','desktop'].includes(active)?'computer':active;
 $$('#morleyDesktopShell .desktop-side-nav [data-target]').forEach(button=>button.classList.toggle('active',button.dataset.target===parent));
}
function labelWorkspace(){
 const header=$('#desktopWorkspaceHeader');if(!header)return;
 const title=$('h1',header);if(title)title.textContent='Morley Buys Dashboard';
 const kicker=$('small',header);if(kicker)kicker.textContent='MORLEY BUYING OPERATIONS';
}
function boot(){
 document.documentElement.classList.add('morley-finance-dashboard');
 renderSidebar();labelWorkspace();syncActive();
 const main=$('main.app')||$('main');
 if(main)new MutationObserver(records=>{if(records.some(record=>record.target.classList?.contains('section')))syncActive();}).observe(main,{subtree:true,attributes:true,attributeFilter:['class']});
 addEventListener('morley-product-parity-ready',()=>{renderSidebar();syncActive();});
}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot,{once:true});else boot();
})();