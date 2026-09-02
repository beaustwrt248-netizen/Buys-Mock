(()=>{
const HISTORY='buysmock_valuation_history_v1';
const $=(s,r=document)=>r.querySelector(s);
const GP={A:.30,B:.50,C:.70,Luxury:.30};
function read(){try{return JSON.parse(localStorage.getItem(HISTORY)||'[]')}catch{return[]}}
function write(v){localStorage.setItem(HISTORY,JSON.stringify(v.slice(0,30)))}
function syncMaxBuy(d){
  const market=Number($('#mswDealMarket',d)?.value)||0;
  const grade=$('#mswDealGrade',d)?.value||'B';
  const max=$('#mswDealMax',d);
  if(!max)return;
  const margin=GP[grade]??GP.B;
  const next=market>0?Math.max(0,market*(1-margin)):0;
  // Preserve cents so decimal market values calculate an exact usable buy ceiling.
  max.value=next?String(Math.round(next*100)/100):'0';
  max.readOnly=true;
  max.setAttribute('aria-readonly','true');
  max.dispatchEvent(new Event('input',{bubbles:true}));
  max.dispatchEvent(new Event('change',{bubbles:true}));
}
function ensure(){
  const d=$('#morleyQuickDealDialog');
  if(!d)return;
  let grade=$('#mswDealGrade',d);
  if(!grade){
    const market=$('#mswDealMarket',d)?.closest('label');
    if(!market)return;
    const label=document.createElement('label');
    label.className='msw-field';
    label.innerHTML='<span>ITEM GRADE</span><select id="mswDealGrade" style="box-sizing:border-box;width:100%;padding:11px 12px;border-radius:12px;border:1px solid rgba(22,199,255,.28);background:#041024;color:#fff"><option value="A">A Grade</option><option value="B" selected>B Grade</option><option value="C">C Grade</option><option value="Luxury">Luxury</option></select>';
    market.insertAdjacentElement('beforebegin',label);
    grade=$('#mswDealGrade',d);
  }
  const marketInput=$('#mswDealMarket',d);
  const maxInput=$('#mswDealMax',d);
  if(maxInput&&!maxInput.dataset.autoMax){
    maxInput.dataset.autoMax='1';
    maxInput.readOnly=true;
    maxInput.setAttribute('aria-readonly','true');
  }
  if(marketInput&&!marketInput.dataset.autoMaxHook){
    marketInput.dataset.autoMaxHook='1';
    marketInput.addEventListener('input',()=>syncMaxBuy(d));
    marketInput.addEventListener('change',()=>syncMaxBuy(d));
  }
  if(grade&&!grade.dataset.autoMaxHook){
    grade.dataset.autoMaxHook='1';
    grade.addEventListener('input',()=>syncMaxBuy(d));
    grade.addEventListener('change',()=>syncMaxBuy(d));
  }
  syncMaxBuy(d);
  const save=$('#mswDealSave',d);
  if(save&&!save.dataset.gradeHook){
    save.dataset.gradeHook='1';
    save.addEventListener('click',()=>{
      const selectedGrade=$('#mswDealGrade',d)?.value||'B';
      const item=$('#mswDealItem',d)?.value.trim()||'';
      const ask=Number($('#mswDealAsk',d)?.value)||0;
      if(!item||!ask)return;
      setTimeout(()=>{
        const h=read();
        const row=h.find(x=>x&&x.type==='deal'&&x.query===item&&Math.abs(Number(x.ask||0)-ask)<0.01&&Date.now()-Number(x.id||0)<10000);
        if(!row)return;
        row.grade=selectedGrade;
        row.itemGrade=selectedGrade;
        write(h);
        window.dispatchEvent(new Event('morley-valuation-history-updated'));
      },0);
    });
  }
}
function decorate(){
  document.querySelectorAll('#morleySmartWorkspace .msw-row').forEach(row=>{
    if(row.querySelector('.morley-grade-pill'))return;
    const text=row.querySelector('.msw-copy b')?.textContent||'';
    const h=read();
    const item=h.find(x=>x?.type==='deal'&&text.includes(x.query||'')&&(x.grade||x.itemGrade));
    if(!item)return;
    const pill=document.createElement('span');
    pill.className='msw-pill morley-grade-pill';
    pill.textContent=`${item.grade||item.itemGrade}${(item.grade||item.itemGrade)==='Luxury'?'':' Grade'}`;
    row.querySelector('.msw-copy')?.appendChild(pill);
  });
}
const obs=new MutationObserver(()=>{ensure();decorate()});
function boot(){obs.observe(document.body,{subtree:true,childList:true});document.addEventListener('click',e=>{if(e.target?.id==='mswNewDeal')setTimeout(ensure,0)});ensure();decorate()}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot);else boot();
})();