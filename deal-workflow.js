(()=>{
const $=id=>document.getElementById(id),KEY='buysmock_valuation_history_v1';
const money=n=>'$'+Math.round(Number(n)||0).toLocaleString('en-AU');
const num=id=>Number((($(id)?.textContent)||'').replace(/[^0-9.]/g,''))||0;
const text=id=>($(id)?.textContent||'').trim();
function load(){try{return JSON.parse(localStorage.getItem(KEY)||'[]')}catch{return[]}}
function save(a){localStorage.setItem(KEY,JSON.stringify(a.slice(0,30)))}
function snapshot(type){
  const isLap=type==='laptop';
  const query=(isLap?($('lapModel')?.value||$('lapSpecs')?.value):($('deskSpecs')?.value||document.querySelector('#parts .pq')?.value)||'').trim();
  if(!query)return;
  const entry={id:Date.now(),type,query,ask:Number($(isLap?'lapAsk':'deskAsk')?.value)||0,newValue:num(isLap?'lapNew':'deskNew'),usedValue:num(isLap?'lapUsed':'deskUsed'),maxBuy:num(isLap?'lapMax':'deskMax'),offer:isLap?num('lapOffer'):0,verdict:text(isLap?'lapVerdict':'deskVerdict')||'—',status:text(isLap?'lapStatus':'deskStatus'),time:new Date().toISOString()};
  if(!entry.newValue&&!entry.usedValue&&!entry.maxBuy)return;
  const a=load();
  const same=a.findIndex(x=>x.type===type&&x.query===query&&Date.now()-new Date(x.time).getTime()<120000);
  if(same>=0)a.splice(same,1);
  a.unshift(entry);save(a);render();
}
function card(){let c=$('valuationHistory');if(c)return c;const home=$('home');if(!home)return null;c=document.createElement('div');c.id='valuationHistory';c.className='card';c.innerHTML='<div style="display:flex;justify-content:space-between;gap:10px;align-items:center"><div><h2 style="margin-bottom:4px">🕘 Recent Valuations</h2><p class="muted" style="margin-top:0">Reopen recent laptop and desktop pricing without searching from scratch.</p></div><button id="clearValHistory" class="secondary">Clear</button></div><div id="valuationHistoryList" class="list"></div>';const quick=$('quickCapture');if(quick)quick.insertAdjacentElement('afterend',c);else home.appendChild(c);$('clearValHistory').onclick=()=>{save([]);render()};return c}
function label(q){const s=String(q||'').replace(/\s+/g,' ').trim();return s.length>80?s.slice(0,77)+'…':s}
function render(){const c=card(),list=$('valuationHistoryList');if(!c||!list)return;const a=load();if(!a.length){list.innerHTML='<div class="item"><small>No valuations saved yet. Analyse a laptop or desktop and it will appear here.</small></div>';return}list.innerHTML=a.slice(0,10).map(x=>`<div class="item" style="align-items:flex-start"><div style="min-width:0;flex:1"><b>${x.type==='laptop'?'💻':'🖥️'} ${escapeHtml(label(x.query))}</b><small style="display:block;margin-top:5px">Used ${money(x.usedValue)} · Max ${money(x.maxBuy)}${x.ask?` · Ask ${money(x.ask)}`:''} · ${escapeHtml(x.verdict||'—')}</small><small style="display:block;margin-top:3px">${new Date(x.time).toLocaleString('en-AU')}</small></div><button class="secondary vhOpen" data-id="${x.id}">Open</button></div>`).join('');list.querySelectorAll('.vhOpen').forEach(b=>b.onclick=()=>openOne(Number(b.dataset.id)))}
function escapeHtml(s){return String(s||'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]))}
function openOne(id){const x=load().find(v=>v.id===id);if(!x)return;if(x.type==='laptop'){window.show?.('laptop');if($('lapModel'))$('lapModel').value=x.query;if($('lapAsk'))$('lapAsk').value=x.ask||'';setTimeout(()=>$('lapAnalyse')?.click(),80)}else{window.show?.('desktop');if($('deskSpecs'))$('deskSpecs').value=x.query;if($('deskAsk'))$('deskAsk').value=x.ask||'';setTimeout(()=>$('deskAnalyse')?.click(),80)}}
function addBuyButtons(){const add=(type,anchorId,existingAddId)=>{const bar=$(anchorId)?.closest('.bar');if(!bar||bar.querySelector(`[data-buy-stock="${type}"]`))return;const b=document.createElement('button');b.dataset.buyStock=type;b.textContent='✓ Buy + Add to Stock';b.className='oneTapStock';b.onclick=()=>{snapshot(type);const addBtn=$(existingAddId);if(addBtn&&!addBtn.disabled){addBtn.click();setTimeout(()=>{if(typeof activity==='function')activity(`${type==='laptop'?'Laptop':'Desktop'} bought and sent to stock`,money(Number($(type==='laptop'?'lapAsk':'deskAsk')?.value)||0))},100)}else{alert('Run a reliable valuation first, then enter the seller asking price before adding to stock.')}};bar.appendChild(b)};add('laptop','lapAnalyse','lapAdd');add('desktop','deskAnalyse','deskAdd')}
function schedule(type){[900,1800,3200,5200].forEach(ms=>setTimeout(()=>snapshot(type),ms))}
function removeRecentActivity(){const recent=$('recent');if(!recent)return;const card=recent.closest('.card');if(card)card.remove();}
function boot(){removeRecentActivity();card();render();addBuyButtons();$('lapAnalyse')?.addEventListener('click',()=>schedule('laptop'));$('deskAnalyse')?.addEventListener('click',()=>schedule('desktop'))}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',()=>setTimeout(boot,300));else setTimeout(boot,300);
})();