(()=>{
function byId(id){return document.getElementById(id)}
function fire(el,type='input'){if(el)el.dispatchEvent(new Event(type,{bubbles:true}))}
function detectType(text){
 const t=String(text||'').toLowerCase();
 if(/macbook|laptop|notebook|thin\s*15|vivobook|zenbook|ideapad|thinkpad|inspiron|latitude|pavilion|victus|omen|legion|predator|nitro|tuf|rog|alienware/.test(t))return'laptop';
 if(/\brtx\s*\d{3,4}|\bgtx\s*\d{3,4}|\brx\s*\d{3,4}|ryzen\s+[3579]|\bi[3579][ -]?\d{4,5}|motherboard|\bb650\b|\bb550\b|\bz790\b|\bddr[45]\b|\bnvme\b|\bpsu\b/.test(t))return'desktop';
 return'general';
}
function injectStyle(){
 const s=document.createElement('style');s.textContent=`
 .quickCapture{margin-top:14px;border:2px solid #ffd400!important;box-shadow:0 14px 34px rgba(0,0,0,.16)}
 .quickCapture h2{margin-bottom:4px}.quickCapture .qcHint{margin:0 0 10px;color:#6b6556}
 .qcGrid{display:grid;grid-template-columns:1.6fr .7fr .7fr;gap:9px;align-items:end}
 .qcAction{min-height:44px}.qcResult{margin-top:10px;padding:10px 12px;border-radius:12px;background:#fff8d5;border:1px solid #e5c400;color:#282300;display:none}
 .qcResult.show{display:block}.quickStrip{display:flex;gap:8px;overflow:auto;padding:2px 0 4px;margin-top:10px}.quickStrip button{white-space:nowrap}
 .oneTapStock{background:#151515!important;color:#ffd400!important;border:1px solid #ffd400!important}
 @media(max-width:760px){.qcGrid{grid-template-columns:1fr}.qcAction{width:100%}}
 `;document.head.appendChild(s)
}
function setupQuickCapture(){
 const home=byId('home');if(!home||byId('quickCapture'))return;
 const hero=home.querySelector('.hero');
 const c=document.createElement('div');c.id='quickCapture';c.className='card quickCapture';
 c.innerHTML=`<h2>⚡ Quick Deal Capture</h2><p class="qcHint">Paste a seller listing, specs or product URL. We’ll route it to the right valuation tool.</p><div class="qcGrid"><div><label>Listing / specs / URL</label><textarea id="qcText" placeholder="Paste the seller listing here…"></textarea></div><div><label>Seller asking price</label><input id="qcAsk" type="number" min="0" inputmode="decimal" placeholder="0"></div><div><label>Type</label><select id="qcType"><option value="auto">Auto detect</option><option value="laptop">Laptop / MacBook</option><option value="desktop">Desktop / Gaming PC</option><option value="general">General Buy</option></select><button id="qcGo" class="qcAction" style="margin-top:8px;width:100%">Analyse Now</button></div></div><div id="qcResult" class="qcResult"></div><div class="quickStrip"><button id="qcLaptop" class="secondary">💻 Laptop</button><button id="qcDesktop" class="secondary">🖥 Desktop</button><button id="qcGP" class="secondary">💰 GP</button><button id="qcStock" class="secondary">📦 Stock</button><button id="qcScan" class="secondary">⌗ Scanner</button></div>`;
 if(hero&&hero.nextSibling)home.insertBefore(c,hero.nextSibling);else home.prepend(c);
 const route=(forced)=>{
  const text=byId('qcText').value.trim(),ask=Number(byId('qcAsk').value)||0,type=forced||((byId('qcType').value==='auto')?detectType(text):byId('qcType').value),out=byId('qcResult');
  if(type==='laptop'){
   if(byId('lapSpecs')){byId('lapSpecs').value=text;fire(byId('lapSpecs'));}
   if(byId('lapAsk')){byId('lapAsk').value=ask||'';fire(byId('lapAsk'));}
   if(typeof window.show==='function')window.show('laptop');
   setTimeout(()=>{if(byId('lapDetect'))byId('lapDetect').click();setTimeout(()=>byId('lapAnalyse')?.click(),80)},100);
   out.textContent='Laptop workflow started.';
  }else if(type==='desktop'){
   if(byId('deskSpecs')){byId('deskSpecs').value=text;fire(byId('deskSpecs'));}
   if(byId('deskAsk')){byId('deskAsk').value=ask||'';fire(byId('deskAsk'));}
   if(typeof window.show==='function')window.show('desktop');
   setTimeout(()=>byId('deskAnalyse')?.click(),100);out.textContent='Desktop workflow started.';
  }else{
   if(typeof window.show==='function')window.show('general');
   if(byId('gpSale')&&ask){byId('gpSale').value=ask;fire(byId('gpSale'));}
   out.textContent='Opened General Buys / GP. Enter the expected resale price if the seller asking price is not the resale value.';
  }
  out.classList.add('show');
 };
 byId('qcGo').onclick=()=>route();byId('qcLaptop').onclick=()=>route('laptop');byId('qcDesktop').onclick=()=>route('desktop');byId('qcGP').onclick=()=>{window.show?.('general')};byId('qcStock').onclick=()=>window.show?.('inventory');byId('qcScan').onclick=()=>window.show?.('scanner');
}
function addWorkflowButtons(){
 const laptopBar=byId('lapAnalyse')?.closest('.bar');
 if(laptopBar&&!byId('lapSaveStock')){const b=document.createElement('button');b.id='lapSaveStock';b.className='oneTapStock';b.textContent='★ Save Deal + Stock';b.onclick=()=>{if(byId('lapSave')&&!byId('lapSave').disabled)byId('lapSave').click();if(byId('lapAdd')&&!byId('lapAdd').disabled)setTimeout(()=>byId('lapAdd').click(),80)};laptopBar.appendChild(b)}
 const deskBar=byId('deskAnalyse')?.closest('.bar');
 if(deskBar&&!byId('deskSaveStock')){const b=document.createElement('button');b.id='deskSaveStock';b.className='oneTapStock';b.textContent='★ Save Deal + Stock';b.onclick=()=>{if(byId('deskSave')&&!byId('deskSave').disabled)byId('deskSave').click();if(byId('deskAdd')&&!byId('deskAdd').disabled)setTimeout(()=>byId('deskAdd').click(),80)};deskBar.appendChild(b)}
}
function boot(){injectStyle();setupQuickCapture();addWorkflowButtons()}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',()=>setTimeout(boot,80));else setTimeout(boot,80);
})();