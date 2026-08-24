(()=>{
  const $=(s,r=document)=>r.querySelector(s);
  const $$=(s,r=document)=>[...r.querySelectorAll(s)];
  const money=v=>new Intl.NumberFormat('en-AU',{style:'currency',currency:'AUD',maximumFractionDigits:0}).format(Number(v)||0);

  function go(page){
    if(typeof window.show==='function') window.show(page);
    else {
      $$('.section').forEach(x=>x.classList.toggle('active',x.id===page));
      $$('nav [data-page]').forEach(x=>x.classList.toggle('active',x.dataset.page===page));
    }
    $$('.desktop-side-nav [data-target]').forEach(x=>x.classList.toggle('active',x.dataset.target===page || (x.dataset.target==='settings' && ['deals','inventory','sales','scanner'].includes(page))));
    window.scrollTo({top:0,behavior:'smooth'});
  }
  window.morleyDesktopGo=go;

  function addDesktopShell(){
    if($('#morleyDesktopShell')) return;
    const shell=document.createElement('aside');
    shell.id='morleyDesktopShell';
    shell.innerHTML=`
      <div class="desktop-brand"><div class="desktop-mark">M</div><div><strong>B&L Morley</strong><span>PRODUCTION</span></div></div>
      <nav class="desktop-side-nav" aria-label="Desktop navigation">
        <button data-target="home"><b>⌂</b><span>Home</span></button>
        <button data-target="laptop"><b>💻</b><span>Laptop</span></button>
        <button data-target="desktop"><b>🖥️</b><span>Desktop</span></button>
        <button data-target="general"><b>$</b><span>GP</span></button>
        <button data-target="settings"><b>⚙</b><span>More</span></button>
      </nav>
      <div class="desktop-side-status"><small>LIVE PRICING</small><strong class="good">READY</strong><small>ONLINE STATUS</small><strong class="good">ONLINE</strong></div>`;
    document.body.appendChild(shell);
    $$('.desktop-side-nav button').forEach(b=>b.onclick=()=>go(b.dataset.target));
    const active=$('.section.active')?.id||'home';
    $(`.desktop-side-nav [data-target="${active}"]`)?.classList.add('active');
  }

  function addDesktopHeader(){
    const app=$('.app'); if(!app||$('#desktopWorkspaceHeader')) return;
    const h=document.createElement('header');
    h.id='desktopWorkspaceHeader';
    h.className='desktop-workspace-header';
    h.innerHTML=`<div><small>B&L MORLEY</small><h1>Buys and Loans Workspace</h1></div><div class="desktop-header-actions"><span class="desktop-pill good-dot">Live pricing</span><span class="desktop-pill good-dot">Online</span></div>`;
    app.prepend(h);
  }

  function addQuickDeal(){
    const home=$('#home'); if(!home||$('#desktopQuickDeal')) return;
    const hero=$('#home .hero');
    const card=document.createElement('div');
    card.id='desktopQuickDeal';
    card.className='card desktop-quick-deal';
    card.innerHTML=`
      <div class="quick-deal-heading"><div><small>⚡ QUICK DEAL CAPTURE</small><h2>Analyse a seller listing</h2><p class="muted">Paste a listing, specifications or model details. B&L Morley will route it to the right valuation tool.</p></div></div>
      <div class="quick-deal-grid">
        <div><label>Listing / specs / URL</label><textarea id="dqText" placeholder="Paste seller listing, model or specifications"></textarea></div>
        <div><label>Seller asking price</label><input id="dqAsk" type="number" min="0" placeholder="0"><label>Valuation type</label><div class="desktop-segment"><button type="button" data-type="auto" class="active">Auto detect</button><button type="button" data-type="laptop">Laptop</button><button type="button" data-type="desktop">Desktop</button></div><button id="dqAnalyse" class="desktop-primary">Analyse Now</button></div>
      </div>`;
    hero?.after(card);
    let type='auto';
    $$('.desktop-segment button',card).forEach(b=>b.onclick=()=>{type=b.dataset.type;$$('.desktop-segment button',card).forEach(x=>x.classList.toggle('active',x===b));});
    $('#dqAnalyse',card).onclick=()=>{
      const text=$('#dqText',card).value.trim(); const ask=$('#dqAsk',card).value;
      if(!text) return;
      const low=text.toLowerCase();
      const isLaptop=type==='laptop'||(type==='auto'&&/(laptop|notebook|macbook|thinkpad|latitude|elitebook|vivobook|ideapad)/i.test(low));
      if(isLaptop){
        const model=$('#lapModel'), specs=$('#lapSpecs'), price=$('#lapAsk');
        if(model) model.value=text.split(/\n|\||,/)[0].slice(0,120);
        if(specs) specs.value=text; if(price&&ask) price.value=ask; go('laptop');
      } else {
        const specs=$('#deskSpecs'), price=$('#deskAsk'); if(specs) specs.value=text; if(price&&ask) price.value=ask; go('desktop');
      }
    };
  }

  function buildMoreHub(){
    const sec=$('#settings'); if(!sec||$('#desktopMoreHub')) return;
    const existing=sec.innerHTML;
    sec.innerHTML=`<div id="desktopMoreHub" class="desktop-more-hub">
      <div class="desktop-page-title"><div><small>MORE</small><h1>Tools & account</h1><p class="muted">Less-used tools and workspace options live here to keep the main valuation flow clean.</p></div></div>
      <div class="desktop-more-grid">
        <button class="desktop-nav-card" data-go="deals"><span>◷</span><div><b>Valuations & Deals</b><small>Search saved valuations and compare opportunities.</small></div></button>
        <button class="desktop-nav-card" data-go="inventory"><span>▣</span><div><b>Inventory</b><small>Stock, costs, resale values and profit tracking.</small></div></button>
        <button class="desktop-nav-card" data-go="sales"><span>↗</span><div><b>Sales History</b><small>Revenue, cost and realised gross profit.</small></div></button>
        <button class="desktop-nav-card" data-go="scanner"><span>⌗</span><div><b>Barcode Scanner</b><small>Find existing stock or quickly add an item.</small></div></button>
      </div>
      <div class="card desktop-account-card"><small>ACCOUNT</small><h2>Desktop Web</h2><p class="muted">Same B&L Morley valuation workspace, optimised for keyboard, mouse and widescreen displays.</p></div>
      <div class="desktop-original-settings">${existing}</div>
    </div>`;
    $$('[data-go]',sec).forEach(b=>b.onclick=()=>go(b.dataset.go));
  }

  function enrichPageTitles(){
    const map={laptop:['💻','Laptop / MacBook','Whole-device Google + eBay AU valuation'],desktop:['🖥️','Desktop / Gaming PC','Component-based live pricing'],general:['$','GP Calculator','A / B / C / Luxury buying targets'],deals:['◷','Valuations & Deals','Saved opportunities and deal tracking'],inventory:['▣','Inventory','Stock, cost, resale and profit'],sales:['↗','Sales History','Revenue and realised profit'],scanner:['⌗','Barcode Scanner','Find or add stock quickly']};
    Object.entries(map).forEach(([id,[icon,title,sub]])=>{
      const sec=$('#'+id); if(!sec||$('.desktop-page-title',sec)) return;
      const t=document.createElement('div');t.className='desktop-page-title';t.innerHTML=`<div><small>${icon} B&L MORLEY</small><h1>${title}</h1><p class="muted">${sub}</p></div>`;sec.prepend(t);
    });
  }

  function addKeyboardShortcuts(){
    document.addEventListener('keydown',e=>{
      if(e.ctrlKey||e.metaKey||e.altKey||/INPUT|TEXTAREA|SELECT/.test(document.activeElement?.tagName||'')) return;
      const k=e.key.toLowerCase(); const m={h:'home',l:'laptop',d:'desktop',g:'general',m:'settings'}; if(m[k]) go(m[k]);
    });
  }

  function init(){
    document.documentElement.classList.add('morley-web-parity');
    addDesktopShell(); addDesktopHeader(); addQuickDeal(); buildMoreHub(); enrichPageTitles(); addKeyboardShortcuts();
    const originalShow=window.show;
    if(typeof originalShow==='function') window.show=function(page){originalShow(page); $$('.desktop-side-nav [data-target]').forEach(x=>x.classList.toggle('active',x.dataset.target===page || (x.dataset.target==='settings'&&['deals','inventory','sales','scanner'].includes(page))));};
  }
  if(document.readyState==='loading') document.addEventListener('DOMContentLoaded',init); else init();
})();
