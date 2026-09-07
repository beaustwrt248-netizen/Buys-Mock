(()=>{
  const nav=document.querySelector('.nav');
  const rename={overview:'Overview',attention:'Research',development:'Task Runner',guardian:'System Health',catalogue:'Catalogue Insights',support:'Support Assistant',knowledge:'Learning & Memory',monitoring:'Web Monitoring',releases:'Reports',recommendations:'Pricing Intelligence',activity:'Activity'};
  document.querySelectorAll('.nav button[data-section]').forEach(b=>{const k=b.dataset.section;if(rename[k])b.textContent=rename[k];});
  const brand=document.querySelector('.brand');if(brand){const eyebrow=brand.querySelector('.eyebrow');const h1=brand.querySelector('h1');if(eyebrow)eyebrow.textContent='NOVA AI';if(h1)h1.textContent='CONTROL CENTER';}
  const overview=document.querySelector('.page[data-page="overview"]');
  if(overview){
    const hero=overview.querySelector('.hero');
    if(hero){const title=hero.querySelector('h2');const summary=hero.querySelector('p');const eyebrow=hero.querySelector('.eyebrow');if(title)title.textContent="Hello Beau, I’m Nova!";if(summary)summary.textContent='Faster · Smarter · Deeper · Always On. Live intelligence across catalogue, pricing, support, releases and protected Guardian analysis.';if(eyebrow)eyebrow.textContent='NOVA AI CONTROL CENTER';
      if(!hero.querySelector('.nova-hero-actions')){const actions=document.createElement('div');actions.className='nova-hero-actions';actions.innerHTML='<button data-nova="ask">Ask Nova</button><button data-open="catalogue">Find Devices</button><button data-open="recommendations">Analyse Pricing</button><button data-open="catalogue">Check Images</button>';hero.firstElementChild.append(actions);actions.querySelectorAll('[data-open]').forEach(b=>b.addEventListener('click',()=>document.querySelector(`.nav button[data-section="${b.dataset.open}"]`)?.click()));}}
    const metricNames=['Live Tasks','Needs Attention','Catalogue Sync','AI Model'];overview.querySelectorAll('.metrics .metric').forEach((m,i)=>{const s=m.querySelector('span');if(s&&metricNames[i])s.textContent=metricNames[i];});
    const oldGrid=overview.querySelector('.grid-two');if(oldGrid)oldGrid.style.display='none';
    if(!overview.querySelector('.nova-dashboard-grid')){const dash=document.createElement('div');dash.className='nova-dashboard-grid';dash.innerHTML=`<article class="card nova-panel"><div class="eyebrow">AUTONOMOUS WORK</div><h2>Live Tasks</h2><div class="task-row"><span>Australian retailer scanning</span><small>Running</small><div class="task-progress"><i style="width:78%"></i></div></div><div class="task-row"><span>Missing image verification</span><small>Active</small><div class="task-progress"><i style="width:63%"></i></div></div><div class="task-row"><span>Pricing intelligence analysis</span><small>Running</small><div class="task-progress"><i style="width:86%"></i></div></div><div class="task-row"><span>Support ticket review</span><small>Live</small><div class="task-progress"><i style="width:58%"></i></div></div><div class="task-row"><span>Device data learning</span><small>Continuous</small><div class="task-progress"><i style="width:91%"></i></div></div><div class="task-row"><span>Web / app monitoring</span><small>Online</small><div class="task-progress"><i style="width:96%"></i></div></div></article><article class="card nova-panel"><div class="eyebrow">NOVA SIGNALS</div><h2>Insights & Suggestions</h2><div class="insight-grid"><div class="insight"><b id="novaMissingDevices">—</b><span>Missing devices</span></div><div class="insight"><b>LIVE</b><span>Pricing signals</span></div><div class="insight"><b>CHECK</b><span>Duplicate records</span></div><div class="insight"><b>SCAN</b><span>Missing images</span></div></div><h2 style="margin-top:22px">System Status</h2><div class="system-list"><div><span>Web Monitoring</span><em>ONLINE</em></div><div><span>Data Research</span><em>ACTIVE</em></div><div><span>Catalogue Sync</span><em>ONLINE</em></div><div><span>AI Model</span><em>READY</em></div><div><span>API Connections</span><em>ONLINE</em></div><div><span>Scheduled Tasks</span><em>ACTIVE</em></div></div></article>`;overview.append(dash);
      const source=document.querySelector('#catalogueModelGaps');const target=dash.querySelector('#novaMissingDevices');if(source&&target){const copy=()=>target.textContent=source.textContent||'—';copy();new MutationObserver(copy).observe(source,{childList:true,subtree:true,characterData:true});}}
  }
  if(!document.querySelector('.nova-command')){
    const command=document.createElement('section');
    command.className='nova-command';
    command.innerHTML='<div class="nova-command-result" id="novaVisualResult" role="status" aria-live="polite"></div><div class="nova-command-row"><input id="novaVisualCommand" placeholder="Ask Nova anything…" aria-label="Ask Nova anything" autocomplete="off"><button id="novaVisualSend" type="button">Ask Nova</button></div><div class="nova-chips"><button data-open="catalogue">Find Devices</button><button data-open="recommendations">Pricing</button><button data-open="support">Support</button><button data-open="monitoring">Monitor</button><button data-open="knowledge">Memory</button><button data-commands>Commands</button></div>';
    document.querySelector('.app-shell main')?.after(command);
    command.querySelectorAll('[data-open]').forEach(b=>b.addEventListener('click',()=>document.querySelector(`.nav button[data-section="${b.dataset.open}"]`)?.click()));
    const send=command.querySelector('#novaVisualSend');
    const input=command.querySelector('#novaVisualCommand');
    const result=command.querySelector('#novaVisualResult');
    const showResult=(message,error=false)=>{result.textContent=message||'';result.className='nova-command-result'+(message?' show':'')+(error?' error':'');};
    const ask=async()=>{
      const q=(input.value||'').trim();
      if(!q)return;
      if(!window.NovaCommands?.execute){showResult('Nova command intelligence is still loading. Try again in a moment.',true);return;}
      send.disabled=true;send.textContent='Working…';showResult('Understanding your request…');
      try{const response=await window.NovaCommands.execute(q);showResult(response?.message||'Nova completed the request.',!response?.ok);if(response?.ok)input.value='';}
      catch(error){showResult(error?.message||'Nova could not complete that command.',true);}
      finally{send.disabled=false;send.textContent='Ask Nova';}
    };
    send.addEventListener('click',ask);
    input.addEventListener('keydown',e=>{if(e.key==='Enter'){e.preventDefault();ask();}});
    command.querySelector('[data-commands]')?.addEventListener('click',()=>window.dispatchEvent(new Event('nova:open-command-browser')));
    document.querySelector('[data-nova="ask"]')?.addEventListener('click',()=>input.focus());
    window.addEventListener('nova:commands-ready',()=>showResult('Nova command intelligence is ready.'));
  }
  const footer=document.querySelector('footer');if(footer)footer.textContent='NOVA AI | Backed by Morley Buys · Smarter Data. Stronger Decisions. · Guardian protected';
})();
