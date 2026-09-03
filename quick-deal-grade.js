(()=>{
const HISTORY='buysmock_valuation_history_v1';
const $=(s,r=document)=>r.querySelector(s);
const MAX_BUY_FACTOR=Object.freeze({A:.70,B:.50,C:.30,Luxury:.70});
const GP_LABEL=Object.freeze({A:'30% GP',B:'50% GP',C:'70% GP',Luxury:'30% GP'});
function syncMaxBuy(d){
  const market=Number($('#mswDealMarket',d)?.value)||0;
  const grade=$('#mswDealGrade',d)?.value||'B';
  const field=$('#mswDealMax',d);
  if(!field)return;
  const maxBuy=Math.round(market*(MAX_BUY_FACTOR[grade]??MAX_BUY_FACTOR.B)*100)/100;
  field.value=market?String(maxBuy):'';
  field.dispatchEvent(new Event('input',{bubbles:true}));
  const note=$('#mswDealGradeNote',d);
  if(note)note.textContent=`${grade} Grade targets ${GP_LABEL[grade]||GP_LABEL.B}. Max Buy is calculated automatically.`;
}
function read(){try{return JSON.parse(localStorage.getItem(HISTORY)||'[]')}catch{return[]}}
function write(v){localStorage.setItem(HISTORY,JSON.stringify(v.slice(0,30)))}
function ensure(){
  const d=$('#morleyQuickDealDialog');
  if(!d||$('#mswDealGrade',d))return;
  const market=$('#mswDealMarket',d)?.closest('label');
  if(!market)return;
  const label=document.createElement('label');
  label.className='msw-field';
  label.innerHTML='<span>ITEM GRADE / TARGET GP</span><select id="mswDealGrade"><option value="A">A — 30% GP</option><option value="B" selected>B — 50% GP</option><option value="C">C — 70% GP</option><option value="Luxury">Luxury — 30% GP</option></select><small id="mswDealGradeNote" style="display:block;margin-top:6px;color:#71827b">B Grade targets 50% GP. Max Buy is calculated automatically.</small>';
  market.insertAdjacentElement('beforebegin',label);
  const max=$('#mswDealMax',d);
  if(max){max.readOnly=true;max.setAttribute('aria-label','Calculated maximum buy price');max.closest('label')?.querySelector('span')?.replaceChildren(document.createTextNode('AUTO MAX BUY'))}
  $('#mswDealMarket',d)?.addEventListener('input',()=>syncMaxBuy(d));
  $('#mswDealGrade',d)?.addEventListener('change',()=>syncMaxBuy(d));
  syncMaxBuy(d);
  const save=$('#mswDealSave',d);
  if(save&&!save.dataset.gradeHook){
    save.dataset.gradeHook='1';
    save.addEventListener('click',()=>{
      const grade=$('#mswDealGrade',d)?.value||'B';
      const item=$('#mswDealItem',d)?.value.trim()||'';
      const ask=Number($('#mswDealAsk',d)?.value)||0;
      const marketValue=Number($('#mswDealMarket',d)?.value)||0;
      if(!item||!ask||!marketValue)return;
      setTimeout(()=>{
        const h=read();
        const row=h.find(x=>x&&x.type==='deal'&&x.query===item&&Math.abs(Number(x.ask||0)-ask)<0.01&&Date.now()-Number(x.id||0)<10000);
        if(!row)return;
        row.grade=grade;
        row.itemGrade=grade;
        row.targetGp=Number((1-(MAX_BUY_FACTOR[grade]??MAX_BUY_FACTOR.B))*100);
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
    pill.textContent=`${item.grade||item.itemGrade} Grade`;
    row.querySelector('.msw-copy')?.appendChild(pill);
  });
}
const obs=new MutationObserver(()=>{ensure();decorate()});
function boot(){obs.observe(document.body,{subtree:true,childList:true});document.addEventListener('click',e=>{if(e.target?.id==='mswNewDeal')setTimeout(ensure,0)});ensure();decorate()}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot);else boot();
})();
