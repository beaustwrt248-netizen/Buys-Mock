(()=>{
  const $=(s,r=document)=>r.querySelector(s), $$=(s,r=document)=>[...r.querySelectorAll(s)];
  const consoleEntries=[
    ['Sony PS4 OG 500 GB',149],['Sony PS4 OG 1 TB',189],['Sony PS4 Slim 500 GB',229],['Sony PS4 Slim 1 TB',269],['Sony PS4 Pro',249],
    ['Sony PS5 Digital',649],['Sony PS5 Disc',699],['Sony PS5 Digital Slim',799],['Sony PS5 Slim Disc',849],['Sony PS5 Pro',1199],
    ['Xbox One (brick)',99],['Xbox One S',179],['Xbox One X',249],['Xbox Series S',329],['Xbox Series X',549],
    ['Nintendo Switch Lite',149],['Nintendo Switch',249],['Nintendo Switch OLED',349],['Nintendo Switch 2',599]
  ];
  const gradePct={A:.70,B:.50,C:.30};
  const money=n=>new Intl.NumberFormat('en-AU',{style:'currency',currency:'AUD',maximumFractionDigits:0}).format(Number(n)||0);

  function go(page){
    if(typeof window.morleyDesktopGo==='function') window.morleyDesktopGo(page);
    else if(typeof window.show==='function') window.show(page);
  }

  function ensureSections(){
    const main=$('main.app')||$('main'); if(!main)return;
    if(!$('#computer')){
      const sec=document.createElement('section'); sec.id='computer'; sec.className='section';
      sec.innerHTML=`<div class="card morley-parity-choice"><small class="morley-kicker">COMPUTER PRICING</small><h2>What are you pricing?</h2><p class="muted">Choose Laptop / MacBook for guided exact-model pricing, or Desktop / Gaming PC for component-based valuation.</p><div class="parity-choice-grid"><button id="parityLaptop" class="parity-choice primary-choice"><span>▱</span><b>Laptop / MacBook</b><small>Guided exact-model Google + eBay AU valuation</small></button><button id="parityDesktop" class="parity-choice"><span>▦</span><b>Desktop / Gaming PC</b><small>Component-based live pricing</small></button></div></div>`;
      main.appendChild(sec);
      $('#parityLaptop').onclick=()=>go('laptop'); $('#parityDesktop').onclick=()=>go('desktop');
    }
    if(!$('#console')){
      const sec=document.createElement('section'); sec.id='console'; sec.className='section';
      const options=consoleEntries.map(([n,v])=>`<option value="${v}">${n} — ${money(v)}</option>`).join('');
      const supported=consoleEntries.map(([n,v])=>`<button type="button" class="console-row" data-console-name="${n.replace(/"/g,'&quot;')}" data-console-value="${v}"><span>${n}</span><b>${money(v)}</b></button>`).join('');
      sec.innerHTML=`<div class="card"><small class="morley-kicker">CONSOLE PRICING</small><h2>Console Pricing</h2><p class="muted">Select a console and condition grade. Buy price follows Morley standard grade rules: A 70% • B 50% • C 30% of the price-sheet value.</p><label>Console</label><select id="consoleModel"><option value="">Choose console</option>${options}</select><label>Grade</label><div class="console-grade-row"><button type="button" data-grade="A" class="active">A Grade</button><button type="button" data-grade="B">B Grade</button><button type="button" data-grade="C">C Grade</button></div></div><div id="consoleResult" class="card console-result" hidden><small>PRICE SHEET VALUE</small><b id="consoleRrp">$0</b><small id="consoleRule">AUTO BUY PRICE • A GRADE (70%)</small><b id="consoleBuy">$0</b><p id="consoleCalc" class="muted"></p></div><div class="card"><h3>Supported consoles</h3><div class="console-list">${supported}</div></div>`;
      main.appendChild(sec);
      let grade='A'; const model=$('#consoleModel'), result=$('#consoleResult');
      const render=()=>{const value=Number(model.value)||0;if(!value){result.hidden=true;return;}const pct=gradePct[grade];result.hidden=false;$('#consoleRrp').textContent=money(value);$('#consoleRule').textContent=`AUTO BUY PRICE • ${grade} GRADE (${Math.round(pct*100)}%)`;$('#consoleBuy').textContent=money(value*pct);$('#consoleCalc').textContent=`${money(value)} × ${Math.round(pct*100)}% = ${money(value*pct)}`;};
      $$('.console-grade-row button',sec).forEach(b=>b.onclick=()=>{grade=b.dataset.grade;$$('.console-grade-row button',sec).forEach(x=>x.classList.toggle('active',x===b));render();});
      model.onchange=render;
      $$('.console-row',sec).forEach(row=>row.onclick=()=>{model.value=row.dataset.consoleValue;render();sec.scrollIntoView({behavior:'smooth',block:'start'});});
    }
  }

  function homeParity(){
    const tiles=$('#home .tiles'); if(!tiles)return;
    const laptop=$('[onclick="show(\'laptop\')"]',tiles), desktop=$('[onclick="show(\'desktop\')"]',tiles);
    if(laptop){laptop.setAttribute('onclick',"show('computer')");laptop.innerHTML='▱<b>Computer Pricing</b><small>Laptop / MacBook or Desktop / Gaming PC</small>';}
    if(desktop){desktop.setAttribute('onclick',"show('console')");desktop.innerHTML='◫<b>Console Pricing</b><small>PS4, PS5, Xbox and Nintendo grade pricing</small>';}
  }

  function navParity(){
    const nav=$('nav');
    if(nav){
      nav.innerHTML=`<button data-page="home" class="active" type="button">⌂<small>Home</small></button><button data-page="computer" type="button">▱<small>Computer</small></button><button data-page="console" type="button">◫<small>Console</small></button><button data-page="general" type="button">$<small>GP</small></button>`;
      $$('#appframe nav button',document).forEach(()=>{});
      $$('button[data-page]',nav).forEach(b=>b.onclick=()=>go(b.dataset.page));
      nav.style.gridTemplateColumns='repeat(4,1fr)';
    }
    $$('.desktop-side-nav [data-target]').forEach(b=>{const t=b.dataset.target;if(t==='laptop'){b.dataset.target='computer';b.innerHTML=b.innerHTML.replace(/Laptop(?:s)?(?:\s*\/\s*MacBooks?)?/i,'Computer Pricing');}else if(t==='desktop'){b.dataset.target='console';b.innerHTML=b.innerHTML.replace(/Desktop(?:s)?(?:\s*\/\s*Gaming PCs?)?/i,'Console Pricing');}});
  }

  function activeNavParity(){
    const observer=new MutationObserver(()=>{
      const active=$('.section.active')?.id||'home';
      const parent=active==='laptop'||active==='desktop'?'computer':active;
      $$('nav [data-page]').forEach(b=>b.classList.toggle('active',b.dataset.page===parent));
    });
    const main=$('main.app')||$('main'); if(main) observer.observe(main,{subtree:true,attributes:true,attributeFilter:['class']});
  }

  function terminologyParity(){
    const gp=$('#general h2'); if(gp) gp.textContent='General buys & GP';
    const lap=$('#laptop h2'); if(lap) lap.textContent='Laptop / MacBook';
    const desk=$('#desktop h2'); if(desk) desk.textContent='Desktop / Gaming PC';
  }

  function addStyles(){
    if($('#productParityStyles'))return;const s=document.createElement('style');s.id='productParityStyles';s.textContent=`
      .morley-kicker{display:block;color:#38d6a3!important;font-size:11px;font-weight:900;letter-spacing:.12em;margin-bottom:8px}
      .parity-choice-grid{display:grid;grid-template-columns:1fr 1fr;gap:12px;margin-top:16px}
      .parity-choice{min-height:150px;text-align:left;display:flex;flex-direction:column;align-items:flex-start;justify-content:center;gap:8px;background:#151d20;border:1px solid #293633;color:#f4f7f6;border-radius:18px;padding:18px}
      .parity-choice span{font-size:25px;color:#38d6a3}.parity-choice b{font-size:18px}.parity-choice small{color:#9eaea9;line-height:1.45}
      .primary-choice{background:linear-gradient(145deg,#15241f,#101619)}
      .console-grade-row{display:grid;grid-template-columns:repeat(3,1fr);gap:8px;margin-top:8px}.console-grade-row button{background:#151d20;border:1px solid #293633;color:#f4f7f6}.console-grade-row button.active{background:#17382f;border-color:#38d6a3;color:#77e9c4}
      .console-result{display:grid;grid-template-columns:1fr 1fr;gap:8px 18px;align-items:end}.console-result small{color:#9eaea9}.console-result b{font-size:28px;color:#f4f7f6}.console-result #consoleBuy{color:#77e9c4}.console-result p{grid-column:1/-1;margin:0}
      .console-list{display:grid;gap:8px}.console-row{width:100%;display:flex;justify-content:space-between;gap:12px;align-items:center;text-align:left;background:#151d20;border:1px solid #293633;color:#f4f7f6;border-radius:14px;padding:13px 14px}.console-row b{color:#77e9c4;white-space:nowrap}
      @media(max-width:650px){.parity-choice-grid{grid-template-columns:1fr}.console-result{grid-template-columns:1fr}.console-result p{grid-column:auto}}
    `;document.head.appendChild(s);
  }

  function boot(){ensureSections();homeParity();navParity();activeNavParity();terminologyParity();addStyles();}
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',()=>setTimeout(boot,0));else setTimeout(boot,0);
  window.addEventListener('load',()=>setTimeout(boot,100));
})();