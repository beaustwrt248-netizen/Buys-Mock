(()=>{'use strict';
const $=(s,r=document)=>r.querySelector(s),$$=(s,r=document)=>[...r.querySelectorAll(s)];
const icon=paths=>`<svg viewBox="0 0 24 24" aria-hidden="true">${paths}</svg>`;
const navItems=[
 ['home',icon('<path d="M3 11.5 12 4l9 7.5"/><path d="M5.5 10v9.5h13V10"/><path d="M9.5 19.5v-6h5v6"/>'),'Dashboard'],
 ['universal',icon('<circle cx="11" cy="11" r="6"/><path d="m16 16 4 4"/>'),'Universal Search'],
 ['phones',icon('<rect x="7" y="2.5" width="10" height="19" rx="2"/><path d="M10 5h4M11 18.5h2"/>'),'Mobile Phones'],
 ['computer',icon('<rect x="3" y="4.5" width="18" height="12" rx="2"/><path d="M8 20h8M12 16.5V20"/>'),'Computer Pricing'],
 ['console',icon('<path d="M7.5 8h9c2.2 0 3.9 1.5 4.2 3.7l.6 4.2c.3 1.9-1.8 3.2-3.3 2.1l-2.4-1.8H8.4L6 18c-1.5 1.1-3.6-.2-3.3-2.1l.6-4.2C3.6 9.5 5.3 8 7.5 8Z"/><path d="M7 11v4M5 13h4M16.5 12h.01M18.5 14h.01"/>'),'Console Pricing'],
 ['general',icon('<circle cx="12" cy="12" r="9"/><path d="M15.5 8.5c-.8-.7-2-1.1-3.3-1.1-2 0-3.4.9-3.4 2.3 0 3.4 6.5 1.7 6.5 4.8 0 1.5-1.5 2.6-3.6 2.6-1.5 0-2.9-.5-3.7-1.3M12 5.5v13"/>'),'General Buys'],
 ['deals',icon('<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3.5 2"/>'),'Saved Deals'],
 ['inventory',icon('<path d="m4 7 8-4 8 4-8 4-8-4Z"/><path d="M4 7v10l8 4 8-4V7M12 11v10"/>'),'Inventory'],
 ['scanner',icon('<path d="M4 8V4h4M16 4h4v4M20 16v4h-4M8 20H4v-4M8 8v8M11 8v8M15 8v8"/>'),'Scanner'],
 ['sales',icon('<path d="M4 19V5M4 19h16"/><path d="m7 15 4-4 3 2 5-6"/><path d="M15 7h4v4"/>'),'Sales'],
 ['settings',icon('<circle cx="6" cy="12" r="1.3"/><circle cx="12" cy="12" r="1.3"/><circle cx="18" cy="12" r="1.3"/>'),'More']
];
function loadScript(src){return new Promise((resolve,reject)=>{if(document.querySelector(`script[data-morley-src="${src}"]`))return resolve();const s=document.createElement('script');s.src=src;s.defer=true;s.dataset.morleySrc=src;s.onload=resolve;s.onerror=reject;document.head.appendChild(s)})}
async function ensureUniversalAssets(){if(!document.querySelector('link[data-morley-universal]')){const l=document.createElement('link');l.rel='stylesheet';l.href='web-universal-buy.css?v=1';l.dataset.morleyUniversal='1';document.head.appendChild(l)}try{await loadScript('morley-central-pricing.js?v=1');await loadScript('web-universal-buy.js?v=1')}catch(error){console.error('Morley Universal Buy assets failed to load',error)}}
function go(page){if(typeof window.morleyDesktopGo==='function')window.morleyDesktopGo(page);else if(typeof window.show==='function')window.show(page);}
function renderSidebar(){const nav=$('#morleyDesktopShell .desktop-side-nav');if(!nav)return;if(nav.dataset.financeDashboard==='1')return;nav.innerHTML=navItems.map(([target,svg,label])=>`<button type="button" data-target="${target}" aria-label="${label}"><span class="morley-dashboard-nav-icon" aria-hidden="true">${svg}</span><span class="morley-dashboard-nav-label">${label}</span></button>`).join('');nav.dataset.financeDashboard='1';$$('button[data-target]',nav).forEach(button=>button.addEventListener('click',()=>go(button.dataset.target)));syncActive()}
function syncActive(){const active=$('.section.active')?.id||'home';const parent=['laptop','desktop'].includes(active)?'computer':active==='universalBuySearch'?'universal':active==='mobilePhones'?'phones':active;$$('#morleyDesktopShell .desktop-side-nav [data-target]').forEach(button=>button.classList.toggle('active',button.dataset.target===parent))}
function labelWorkspace(){const header=$('#desktopWorkspaceHeader');if(!header)return;const title=$('h1',header);if(title)title.textContent='Morley Buys Dashboard';const kicker=$('small',header);if(kicker)kicker.textContent='MORLEY BUYING OPERATIONS'}
function boot(){document.documentElement.classList.add('morley-finance-dashboard');renderSidebar();labelWorkspace();syncActive();ensureUniversalAssets();const main=$('main.app')||$('main');if(main)new MutationObserver(records=>{if(records.some(record=>record.target.classList?.contains('section')))syncActive()}).observe(main,{subtree:true,attributes:true,attributeFilter:['class']});addEventListener('morley-product-parity-ready',()=>{renderSidebar();syncActive()});addEventListener('morley-universal-buy-ready',()=>{const nav=$('#morleyDesktopShell .desktop-side-nav');if(nav)delete nav.dataset.financeDashboard;renderSidebar();syncActive()})}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot,{once:true});else boot();
})();