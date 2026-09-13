(()=>{
'use strict';
const DESKTOP=1000;
const $=(s,r=document)=>r.querySelector(s);
const $$=(s,r=document)=>Array.from(r.querySelectorAll(s));
const icon=(name)=>({dashboard:'⌂',search:'⌕',devices:'▣',catalogue:'▤',stock:'□',sales:'↗',trade:'↔',price:'$',ai:'✦',reports:'▥',settings:'⚙',scan:'⌗',bell:'♢',plus:'+',book:'▤',tag:'◇',help:'?',menu:'☰'}[name]||'•');

const nav=[
 ['dashboard','Dashboard','dashboard','dashboard'],
 ['search','Search & Scan','search','search'],
 ['devices','Devices','devices','catalogue'],
 ['catalogue','Catalogue','catalogue','catalogue'],
 ['stock','Stock','stock','inventory'],
 ['sales','Sales','sales','sales'],
 ['trade','Trade In / Buy','trade','trade'],
 ['price','Price Check','price','price'],
 ['ai','AI Insights','ai','ai'],
 ['reports','Reports','reports','reports'],
 ['settings','Settings','settings','settings']
];

const capabilityFunctions={
 notifications:['MorleyNotifications.open'],
 scanner:['morleyOpenScanner','openScanner'],
 catalogue:['morleyOpenCatalogue'],
 account:['morleyOpenAccount'],
 support:['morleyOpenSupport','morleyOpenDiagnostics']
};
const capabilities={
 dashboard:{sections:['home'],fallback:'home'},
 search:{sections:['universal'],fallback:'universal'},
 scanner:{selectors:['[data-route="scanner"]','#scanDevice','#openScanner'],sections:['scanner'],fallback:'universal'},
 catalogue:{sections:['categories'],fallback:'categories'},
 inventory:{sections:['inventory'],fallback:'inventory'},
 sales:{sections:['sales'],fallback:'sales'},
 trade:{sections:['general'],fallback:'general'},
 price:{sections:['universal'],fallback:'universal'},
 ai:{handler:openInsightsPanel,fallback:'home'},
 reports:{handler:exportReport,fallback:'sales'},
 settings:{sections:['settings'],fallback:'settings'},
 notifications:{handler:()=>window.MorleyNotifications?.open?.(),fallback:'settings'},
 account:{handler:openAccount,fallback:'settings'},
 support:{handler:openSupport,fallback:'settings'}
};

const categoryConfig=[
 ['Mobile Phones','mobilePhones','▯','mobile phone'],
 ['Laptops','laptop','▱','laptop'],
 ['Tablets','catalogue','▯','tablet'],
 ['Consoles','console','✚','console'],
 ['Smartwatches','catalogue','◉','smartwatch'],
 ['Headphones','catalogue','◖','headphones'],
 ['Cameras','catalogue','◉','camera'],
 ['More','catalogue','•••','']
];

let raf=0;
let booted=false;
let mainObserver=null;
let refreshQueued=false;

function money(v){const n=Number(v)||0;return new Intl.NumberFormat('en-AU',{style:'currency',currency:'AUD',maximumFractionDigits:0}).format(n)}
function num(id){return Number(String($(id)?.textContent||'0').replace(/[^0-9.-]/g,''))||0}
function escapeHtml(v){const d=document.createElement('div');d.textContent=String(v||'');return d.innerHTML}
function invokePath(path){const parts=String(path||'').split('.');let ctx=window;for(let i=0;i<parts.length-1;i++){ctx=ctx?.[parts[i]];if(!ctx)return false}const fn=ctx?.[parts.at(-1)];if(typeof fn!=='function')return false;fn.call(ctx);return true}
function showSection(section){if(!section)return false;const el=document.getElementById(section);if(!el)return false;if(typeof window.morleyDesktopGo==='function')window.morleyDesktopGo(section);else if(typeof window.show==='function')window.show(section);else return false;queueRefresh();return true}
function clickExisting(selectors=[]){for(const selector of selectors){const el=$(selector);if(el&&typeof el.click==='function'){el.click();queueRefresh();return true}}return false}
function resolveCapability(name){return capabilities[name]||null}
function openCapability(name,options={}){
 const cap=resolveCapability(name);if(!cap)return false;
 if(typeof cap.handler==='function'){
  try{const result=cap.handler(options);if(result!==false){queueRefresh();return true}}catch(error){console.warn(`Morley desktop capability ${name} failed`,error)}
 }
 for(const fn of capabilityFunctions[name]||[])if(invokePath(fn))return true;
 if(clickExisting(cap.selectors))return true;
 for(const section of cap.sections||[])if(showSection(section))return true;
 return cap.fallback?showSection(cap.fallback):false;
}
function go(key){if(key==='dashboard')openCapability('dashboard');else openCapability(key);syncActive()}
function action(label){
 const map={'Scan Device':'scanner','Manual Search':'search','Add New Device':'inventory','Add Stock':'inventory','Check Price':'price','View Catalogue':'catalogue','Generate Report':'reports'};
 const capability=map[label]||'dashboard';openCapability(capability);
 if(label==='Add New Device'||label==='Add Stock')setTimeout(()=>$('#addItem')?.click(),80);
}
function safeArray(key,fallback){try{const value=JSON.parse(localStorage.getItem(key)||localStorage.getItem(fallback)||'[]');return Array.isArray(value)?value:[]}catch(_){return []}}
function exportReport(){
 const inventory=safeArray('inventory','morleyInventory');const sales=safeArray('sales','morleySales');
 const rows=[['Morley Buys Report',new Date().toLocaleString('en-AU')],['Stock units',num('#dUnits')],['Stock cost',num('#dCost')],['Potential sales',num('#dSales')],['Potential profit',num('#dProfit')],['Sales',num('#dSaleCount')],['Realised profit',num('#dRealProfit')],[],['Inventory records',inventory.length],['Sales records',sales.length]];
 const csv=rows.map(r=>r.map(v=>'"'+String(v??'').replace(/"/g,'""')+'"').join(',')).join('\n');const blob=new Blob([csv],{type:'text/csv'});const url=URL.createObjectURL(blob);const a=document.createElement('a');a.href=url;a.download='morley-buys-report-'+new Date().toISOString().slice(0,10)+'.csv';document.body.appendChild(a);a.click();a.remove();setTimeout(()=>URL.revokeObjectURL(url),500);return true
}
function globalSearch(){const q=$('#morleyDesktopGlobalSearch')?.value.trim();if(!q)return;openCapability('search');setTimeout(()=>{const candidates=['#universalSearch','#morleyUniversalSearch','#productSearch','#dealSearch','#invSearch'];for(const s of candidates){const el=$(s);if(el){el.value=q;el.dispatchEvent(new Event('input',{bubbles:true}));el.dispatchEvent(new Event('change',{bubbles:true}));el.focus();break}}},100)}
function applyCatalogueQuery(query){openCapability('catalogue');if(!query)return;setTimeout(()=>{for(const selector of ['#categorySearch','#catalogueSearch','#universalSearch','#productSearch']){const el=$(selector);if(el){el.value=query;el.dispatchEvent(new Event('input',{bubbles:true}));el.dispatchEvent(new Event('change',{bubbles:true}));break}}},100)}
function openCategory(target,query){if(['mobilePhones','laptop','console'].includes(target)&&document.getElementById(target)){showSection(target);return}applyCatalogueQuery(query)}
function openAccount(){const existing=$('.morley-web-user');if(existing){existing.click?.();if(existing.closest('button,a'))return true}return showSection('settings')}
function openSupport(){for(const selector of ['[data-web-issue-report="1"]','#morleyReportIssue','#morleyDiagnostics','[data-route="diagnostics"]']){const el=$(selector);if(el){el.click?.();return true}}return showSection('settings')}
function openInsightsPanel(){
 showSection('home');renderDesktop();let panel=$('#morleyDesktopInsightsPanel');if(!panel){panel=document.createElement('section');panel.id='morleyDesktopInsightsPanel';panel.className='mdr-card mdr-insights-panel';$('#morleyDesktopDashboard')?.prepend(panel)}
 if(!panel)return false;const history=safeArray('buysmock_valuation_history_v1','morleyValuationHistory');const latest=history[0];const potential=num('#dProfit');const sales=num('#dSales');panel.innerHTML=`<header><h2>AI Insights</h2><button type="button" data-mdr-close-insights aria-label="Close AI Insights">×</button></header><p class="mdr-insight-copy">${latest?`Latest valuation: ${escapeHtml(latest.query||'Recent valuation')}.`: 'Run a valuation to populate confidence insights.'}</p><div class="mdr-market-list"><div><span>Potential profit</span><b>${money(potential)}</b></div><div><span>Potential sales</span><b>${money(sales)}</b></div><div><span>Valuation evidence</span><b>${history.length} records</b></div></div>`;panel.scrollIntoView({block:'start',behavior:'smooth'});return true
}
function notificationCount(){try{return Number(window.MorleyNotifications?.unread?.())||0}catch{return 0}}
function accountLabel(){const el=$('.morley-web-user');return (el?.textContent||'').trim()||localStorage.getItem('morley_profile_name')||'Morley'}
function initials(name){return String(name||'Morley').split(/\s+/).filter(Boolean).slice(0,2).map(x=>x[0]?.toUpperCase()||'').join('')||'MB'}

function navMarkup(){return nav.map(([key,label,i])=>`<button class="mdr-nav-item" type="button" data-mdr-route="${key}" aria-label="${label}"><span class="mdr-nav-icon">${icon(i)}</span><span>${label}</span></button>`).join('')}
function dashboardMarkup(){const units=num('#dUnits'),cost=num('#dCost'),sales=num('#dSales'),profit=num('#dProfit'),sold=num('#dSaleCount'),deals=num('#dDeals');const avg=units?Math.round(sales/units):0;return `
<div class="mdr-dashboard" id="morleyDesktopDashboard">
 <section class="mdr-hero">
  <div class="mdr-hero-copy"><span class="mdr-eyebrow">MORLEY BUYING OPERATIONS</span><h1>Welcome to<br>Morley Buys</h1><p>Scan. Search. Compare. Buy Smarter.</p>
   <div class="mdr-hero-actions"><button data-mdr-action="Scan Device"><b>${icon('scan')}</b><span>Scan Device</span></button><button data-mdr-action="Manual Search"><b>${icon('search')}</b><span>Manual Search</span></button><button data-mdr-action="Add Stock"><b>${icon('plus')}</b><span>Add Stock</span></button><button data-mdr-action="View Catalogue"><b>${icon('book')}</b><span>View Catalogue</span></button></div>
  </div>
  <div class="mdr-device-art" aria-hidden="true"><span class="mdr-laptop"></span><span class="mdr-phone"></span><span class="mdr-tablet"></span><span class="mdr-controller">✚</span><small>All Your<br><b>Devices in One Place</b><em>Mobiles · Tablets · Laptops<br>Consoles · Smart Tech · & More</em></small></div>
 </section>
 <section class="mdr-kpis">
  <article><i>▣</i><div><small>Total Devices</small><strong data-mdr-kpi="units">${units.toLocaleString()}</strong><span>live stock</span></div></article>
  <article><i>◇</i><div><small>Avg. Market Value</small><strong data-mdr-kpi="avg">${money(avg)}</strong><span>live valuation</span></div></article>
  <article><i>$</i><div><small>Total Stock Value</small><strong data-mdr-kpi="cost">${money(cost)}</strong><span>current inventory</span></div></article>
  <article><i>▥</i><div><small>Items Sold</small><strong data-mdr-kpi="sold">${sold.toLocaleString()}</strong><span data-mdr-kpi="deals">${deals} saved deals</span></div></article>
 </section>
 <div class="mdr-main-grid">
  <div class="mdr-left-column">
   <section class="mdr-card"><header><h2>Device Categories</h2><button data-mdr-route="devices">View All</button></header><div class="mdr-categories">${categoryConfig.map(([label,target,ic,query])=>`<button data-mdr-target="${target}" data-mdr-query="${query}" aria-label="Open ${label}"><b>${ic}</b><span>${label}</span></button>`).join('')}</div></section>
   <div class="mdr-two-col">
    <section class="mdr-card"><header><h2>Recent Activity</h2><button data-mdr-route="stock">View All</button></header><div class="mdr-recent" id="mdrRecent"></div></section>
    <section class="mdr-card mdr-market"><header><h2>Live Market Snapshot</h2><button data-mdr-route="price">Check Price</button></header><div class="mdr-market-list"><div><span>Potential sales</span><b data-mdr-market="sales">${money(sales)}</b></div><div><span>Potential profit</span><b data-mdr-market="profit">${money(profit)}</b></div><div><span>Stock cost</span><b data-mdr-market="cost">${money(cost)}</b></div><div><span>Pricing engine</span><b class="mdr-green" data-mdr-market="api">${$('#apiStatus')?.textContent||'READY'}</b></div></div></section>
   </div>
   <section class="mdr-card mdr-stock-overview"><header><h2>Current Stock Snapshot</h2><span class="mdr-legend">Current relative stock/value view</span></header><div class="mdr-chart" role="img" aria-label="Current Morley stock value snapshot"><div class="mdr-chart-line"></div><div class="mdr-bars"><i style="height:32%"></i><i style="height:46%"></i><i style="height:39%"></i><i style="height:55%"></i><i style="height:50%"></i><i style="height:65%"></i><i style="height:72%"></i><i style="height:82%"></i></div></div></section>
  </div>
  <aside class="mdr-right-column">
   <section class="mdr-card mdr-quick"><h2>Quick Actions</h2>${['Scan Device','Manual Search','Add New Device','Check Price','View Catalogue','Generate Report'].map((x,i)=>`<button data-mdr-action="${x}"><b>${[icon('scan'),icon('search'),icon('plus'),icon('tag'),icon('book'),'⇩'][i]}</b><span>${x}</span><i>›</i></button>`).join('')}</section>
   <section class="mdr-card mdr-brands"><header><h2>Top Categories</h2><button data-mdr-route="devices">View All</button></header><div><span>Mobile Phones</span><i><b style="width:92%"></b></i></div><div><span>Laptops</span><i><b style="width:78%"></b></i></div><div><span>Consoles</span><i><b style="width:61%"></b></i></div><div><span>Tablets</span><i><b style="width:47%"></b></i></div></section>
   <section class="mdr-card mdr-help"><b>${icon('help')}</b><div><h3>Need Help?</h3><p>Open settings, diagnostics or support tools.</p></div><button data-mdr-capability="support">Help Centre</button></section>
  </aside>
 </div>
</div>`}
function recentRows(){const host=$('#mdrRecent');if(!host)return;const source=$('#recent');let rows=[];if(source)rows=$$('.item',source).slice(0,5).map((el,i)=>({title:el.querySelector('b,strong')?.textContent?.trim()||el.textContent.trim().slice(0,45),detail:el.querySelector('small')?.textContent?.trim()||'Recent Morley activity',time:i===0?'Just now':`${i*12} min ago`}));if(!rows.length)rows=[{title:'No recent activity yet',detail:'Scans, deals and stock activity will appear here.',time:'—'}];host.innerHTML=rows.map(r=>`<button type="button" data-mdr-route="stock"><span class="mdr-row-thumb">▣</span><span><b>${escapeHtml(r.title)}</b><small>${escapeHtml(r.detail)}</small></span><time>${r.time}</time><i>›</i></button>`).join('')}

function installShell(){
 if(innerWidth<DESKTOP||document.documentElement.classList.contains('morley-physical-phone'))return teardown();
 document.documentElement.classList.add('morley-desktop-rebuilt');let shell=$('#morleyDesktopRebuildShell');
 if(!shell){shell=document.createElement('div');shell.id='morleyDesktopRebuildShell';shell.innerHTML=`<aside class="mdr-sidebar"><div class="mdr-brand"><span>🛒</span><b>Morley Buys</b></div><nav class="mdr-sidebar-nav">${navMarkup()}</nav><div class="mdr-version"><b>🛒</b><span><strong>Morley Buys</strong><small>Desktop Workspace</small></span></div><small class="mdr-tagline">Buy Smarter. Sell Better.</small></aside><div class="mdr-topbar"><button class="mdr-hamburger" type="button" aria-label="Toggle navigation">${icon('menu')}</button><form class="mdr-search" id="morleyDesktopSearchForm"><input id="morleyDesktopGlobalSearch" autocomplete="off" aria-label="Search Morley Buys" placeholder="Search for devices, scan a barcode, or enter a model number..."><button type="submit" aria-label="Search">${icon('search')}</button></form><button class="mdr-scan-top" type="button" data-mdr-capability="scanner" aria-label="Scan Device">${icon('scan')} <span>Scan Device</span></button><button class="mdr-notify" type="button" data-mdr-capability="notifications" aria-label="Notifications">${icon('bell')}<i id="morleyDesktopNotificationBadge" hidden></i></button><button class="mdr-profile" type="button" data-mdr-capability="account" aria-label="Open account menu"><b id="morleyDesktopProfileInitials">MB</b><span><strong id="morleyDesktopProfileName">Morley</strong><small>Workspace</small></span><i>⌄</i></button></div>`;document.body.appendChild(shell);bindShell(shell)}
 renderDesktop();refreshDesktopState()
}
function bindShell(root){if(root.dataset.mdrBound==='1')return;root.dataset.mdrBound='1';root.addEventListener('click',e=>{const route=e.target.closest('[data-mdr-route]')?.dataset.mdrRoute;if(route)go(route);const capability=e.target.closest('[data-mdr-capability]')?.dataset.mdrCapability;if(capability)openCapability(capability);if(e.target.closest('.mdr-hamburger'))root.querySelector('.mdr-sidebar')?.classList.toggle('collapsed')});$('#morleyDesktopSearchForm',root)?.addEventListener('submit',e=>{e.preventDefault();globalSearch()})}
function parkLegacyHome(home){let legacy=$('#morleyDesktopLegacyHome',home);if(legacy)return legacy;legacy=document.createElement('div');legacy.id='morleyDesktopLegacyHome';legacy.hidden=true;const nodes=Array.from(home.childNodes);for(const node of nodes)legacy.appendChild(node);home.appendChild(legacy);return legacy}
function renderDesktop(){if(innerWidth<DESKTOP)return;const home=$('#home');if(!home)return;if(!$('#morleyDesktopDashboard',home)){parkLegacyHome(home);home.insertAdjacentHTML('beforeend',dashboardMarkup());bindDashboard($('#morleyDesktopDashboard',home))}refreshDesktopState();syncActive()}
function bindDashboard(root){if(!root||root.dataset.mdrBound==='1')return;root.dataset.mdrBound='1';root.addEventListener('click',e=>{if(e.target.closest('[data-mdr-close-insights]')){$('#morleyDesktopInsightsPanel')?.remove();return}const route=e.target.closest('[data-mdr-route]')?.dataset.mdrRoute;if(route)go(route);const target=e.target.closest('[data-mdr-target]');if(target)openCategory(target.dataset.mdrTarget,target.dataset.mdrQuery||'');const capability=e.target.closest('[data-mdr-capability]')?.dataset.mdrCapability;if(capability)openCapability(capability);const act=e.target.closest('[data-mdr-action]')?.dataset.mdrAction;if(act)action(act)})}
function ownerForSection(active){if(active==='home')return'dashboard';if(['laptop','desktop','mobilePhones','console','categories'].includes(active))return'devices';if(active==='universal')return'search';if(active==='inventory')return'stock';if(active==='sales')return'sales';if(active==='general')return'trade';if(active==='settings')return'settings';if(active==='scanner')return'search';return'dashboard'}
function syncActive(){const active=$('.section.active')?.id||'home';const key=ownerForSection(active);$$('.mdr-nav-item').forEach(b=>b.classList.toggle('active',b.dataset.mdrRoute===key));document.body.classList.toggle('mdr-on-dashboard',active==='home')}
function refreshDesktopState(){
 if(innerWidth<DESKTOP||!$('#morleyDesktopRebuildShell'))return;
 const units=num('#dUnits'),cost=num('#dCost'),sales=num('#dSales'),profit=num('#dProfit'),sold=num('#dSaleCount'),deals=num('#dDeals'),avg=units?Math.round(sales/units):0;
 const values={units:units.toLocaleString(),avg:money(avg),cost:money(cost),sold:sold.toLocaleString(),deals:`${deals} saved deals`};for(const [key,value] of Object.entries(values)){const el=$(`[data-mdr-kpi="${key}"]`);if(el)el.textContent=value}
 const market={sales:money(sales),profit:money(profit),cost:money(cost),api:$('#apiStatus')?.textContent||'READY'};for(const [key,value] of Object.entries(market)){const el=$(`[data-mdr-market="${key}"]`);if(el)el.textContent=value}
 recentRows();const count=notificationCount(),badge=$('#morleyDesktopNotificationBadge');if(badge){badge.textContent=count>99?'99+':String(count);badge.hidden=count<=0}
 const name=accountLabel();const nameEl=$('#morleyDesktopProfileName'),initialEl=$('#morleyDesktopProfileInitials');if(nameEl)nameEl.textContent=name;if(initialEl)initialEl.textContent=initials(name)
}
function queueRefresh(){if(refreshQueued)return;refreshQueued=true;requestAnimationFrame(()=>{refreshQueued=false;syncActive();if($('.section.active')?.id==='home')renderDesktop();else refreshDesktopState()})}
function restoreLegacyHome(){const home=$('#home');if(!home)return;$('#morleyDesktopDashboard',home)?.remove();const legacy=$('#morleyDesktopLegacyHome',home);if(!legacy)return;while(legacy.firstChild)home.insertBefore(legacy.firstChild,legacy);legacy.remove()}
function teardown(){document.documentElement.classList.remove('morley-desktop-rebuilt');$('#morleyDesktopRebuildShell')?.remove();restoreLegacyHome()}
function schedule(){cancelAnimationFrame(raf);raf=requestAnimationFrame(()=>{installShell();syncActive()})}
function boot(){if(booted)return;booted=true;installShell();syncActive();addEventListener('resize',schedule,{passive:true});addEventListener('storage',queueRefresh);addEventListener('morley-product-parity-ready',schedule);addEventListener('morley-universal-buy-ready',schedule);addEventListener('morley-profile-updated',queueRefresh);addEventListener('morley-valuation-history-updated',queueRefresh);const main=$('main.app')||$('main');if(main&&!mainObserver){mainObserver=new MutationObserver(queueRefresh);mainObserver.observe(main,{subtree:true,attributes:true,childList:true,attributeFilter:['class']})}}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot,{once:true});else boot();
})();
