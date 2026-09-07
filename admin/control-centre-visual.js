(()=>{
  const byTab=(name)=>document.querySelector(`.tab[data-tab="${name}"]`);
  const activate=(name)=>{const el=byTab(name);if(el)el.click();};
  const labels={overview:'Dashboard',users:'Users & Permissions',devices:'Inventory',tickets:'Support Tickets',pricing:'Pricing',controls:'Settings',release:'Integrations',notify:'Notifications',announce:'Announcements',audit:'Activity Log'};
  document.querySelectorAll('.tab[data-tab]').forEach(btn=>{const k=btn.dataset.tab;if(labels[k])btn.textContent=labels[k];});
  const guardian=document.querySelector('.tabs a[href="guardian.html"]');if(guardian){guardian.textContent='Nova / Guardian';guardian.setAttribute('aria-label','Open Nova and Guardian controls');}
  const top=document.querySelector('.topbar');if(top){const title=top.querySelector('h1');const eyebrow=top.querySelector('.eyebrow');if(title)title.textContent='ADMIN CONTROL';if(eyebrow)eyebrow.textContent='MORLEY BUYS';}
  const tabs=document.querySelector('.tabs');if(tabs&&!document.querySelector('.admin-nova-brand')){const brand=document.createElement('div');brand.className='admin-nova-brand';brand.innerHTML='<b>Backed by NOVA AI</b><span>Smarter Tools · Bigger Results</span>';tabs.after(brand);}
  const overview=document.querySelector('#tab-overview');
  if(overview&&!overview.querySelector('.admin-visual-hero')){
    const hero=document.createElement('section');hero.className='admin-visual-hero';hero.innerHTML='<div class="eyebrow">CONTROL CENTRE</div><h2>Welcome Back, Beau!</h2><p>Manage the Morley catalogue, pricing, listings, support, users and releases from one live command centre.</p><div class="quick-actions"><button data-go="devices">Add Device</button><button data-go="pricing">Update Pricing</button><button data-go="controls">Sync Listings</button><button data-go="audit">View Reports</button></div>';
    overview.prepend(hero);
    hero.querySelectorAll('[data-go]').forEach(b=>b.addEventListener('click',()=>activate(b.dataset.go)));
    const metrics=overview.querySelectorAll(':scope > .grid .metric');
    const names=['Users & Access','Total Devices','Current Release','Pending Updates'];metrics.forEach((m,i)=>{const s=m.querySelector('span');if(s&&names[i])s.textContent=names[i];});
    const panels=document.createElement('div');panels.className='overview-panels';panels.innerHTML='<article class="card overview-panel"><h3>Inventory Overview</h3><div class="inventory-bars" aria-label="Inventory overview"><i style="height:44%"></i><i style="height:66%"></i><i style="height:57%"></i><i style="height:82%"></i><i style="height:74%"></i><i style="height:91%"></i><i style="height:69%"></i><i style="height:88%"></i></div></article><article class="card overview-panel"><h3>Listing Status</h3><div class="status-ring" aria-label="Live listing status"></div><p class="muted" style="text-align:center">Live catalogue and listing services</p></article>';
    overview.append(panels);
    const lower=document.createElement('div');lower.className='admin-subgrid';lower.innerHTML='<article class="card overview-panel"><h3>Recent Activity</h3><div class="activity-feed"><div>Catalogue intelligence connected</div><div>Protected pricing controls ready</div><div>Support and release monitoring active</div></div></article><article class="card overview-panel"><h3>Quick Links</h3><div class="quick-links"><button data-go="devices">Catalogue & Inventory</button><button data-go="tickets">Support Tickets</button><button data-go="users">Users & Permissions</button><button data-go="release">Release Control</button></div></article>';
    overview.append(lower);lower.querySelectorAll('[data-go]').forEach(b=>b.addEventListener('click',()=>activate(b.dataset.go)));
  }
})();
