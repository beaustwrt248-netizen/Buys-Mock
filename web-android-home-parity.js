(()=>{
'use strict';
const H='buysmock_valuation_history_v1',W='morley_watchlist_v1';
const $=(s,r=document)=>r.querySelector(s), $$=(s,r=document)=>[...r.querySelectorAll(s)];
const mobile=()=>matchMedia('(max-width:760px)').matches;
const read=(k,f=[])=>{try{return JSON.parse(localStorage.getItem(k)||JSON.stringify(f))}catch{return f}};
const money=n=>new Intl.NumberFormat('en-AU',{style:'currency',currency:'AUD',maximumFractionDigits:0}).format(Number(n)||0);
const margin=x=>Math.max(0,(Number(x.usedValue)||0)-(Number(x.ask)||0));
const verdict=x=>{const ask=Number(x.ask)||0,max=Number(x.maxBuy)||0,market=Number(x.usedValue)||0;if(!ask)return'REVIEW';if(max&&ask<=max)return'GREAT BUY';if(market&&ask<=market*.78)return'GOOD BUY';if(market&&ask<market)return'MARGINAL';return'AVOID'};
let queued=false,observer=null;

function addCopy(root){
  if(root.querySelector('.morley-android-home-copy'))return;
  const head=root.querySelector('.msw-head');
  if(!head)return;
  const p=document.createElement('p');
  p.className='morley-android-home-copy';
  p.textContent='Recent valuations, watchlist activity and buy opportunities at a glance.';
  const status=head.querySelector('.msw-status');
  status?status.insertAdjacentElement('beforebegin',p):head.appendChild(p);
}

function metrics(root){
  const cards=$$('.msw-grid .msw-metric',root);
  if(!cards.length)return;
  const labels=['VALUATIONS','WATCHLIST','OPPORTUNITIES'];
  cards.slice(0,3).forEach((card,i)=>{const label=card.querySelector('small');if(label)label.textContent=labels[i]});
  cards.slice(3).forEach(card=>card.remove());
  const grid=root.querySelector('.msw-grid');
  if(!grid||grid.nextElementSibling?.classList.contains('morley-potential-margin'))return;
  const history=read(H), opportunities=history.filter(x=>['GREAT BUY','GOOD BUY'].includes(verdict(x)));
  const total=opportunities.reduce((sum,x)=>sum+margin(x),0);
  const panel=document.createElement('div');
  panel.className='morley-potential-margin';
  panel.innerHTML=`<div><small>POTENTIAL GROSS MARGIN</small><b>${money(total)}</b></div><span>${opportunities.length} live buy ${opportunities.length===1?'opportunity':'opportunities'}</span>`;
  grid.insertAdjacentElement('afterend',panel);
}

function actions(root){
  const actions=root.querySelector('.msw-actions');
  if(!actions)return;
  const quick=root.querySelector('#mswNewDeal');
  if(quick)quick.textContent='Quick Deal Mode';
  const deals=root.querySelector('#mswOpenDeals');
  if(deals)deals.textContent='Open Valuations & Deals';
  if(!root.querySelector('#morleyWebNfcInfo')){
    const nfc=document.createElement('button');
    nfc.id='morleyWebNfcInfo';
    nfc.type='button';
    nfc.className='msw-action morley-android-only';
    nfc.disabled=true;
    nfc.setAttribute('aria-disabled','true');
    nfc.textContent='NFC Scanner · Android only';
    actions.appendChild(nfc);
  }
}

function refresh(root){
  if(root.querySelector('#morleyHomeRefresh'))return;
  const button=document.createElement('button');
  button.id='morleyHomeRefresh';
  button.type='button';
  button.className='morley-home-refresh';
  button.textContent='Refresh Smart Workspace';
  button.addEventListener('click',()=>{
    window.dispatchEvent(new Event('morley-valuation-history-updated'));
    window.dispatchEvent(new Event('morley-watchlist-updated'));
    window.dispatchEvent(new Event('storage'));
  });
  root.appendChild(button);
}

function apply(){
  if(!mobile())return;
  const root=$('#morleySmartWorkspace');
  if(!root)return;
  addCopy(root);metrics(root);actions(root);refresh(root);
  document.documentElement.dataset.morleyAndroidHomeParity='1';
}
function schedule(){if(queued)return;queued=true;(requestAnimationFrame||setTimeout)(()=>{queued=false;apply()})}
function boot(){
  apply();
  observer=new MutationObserver(m=>{if(m.some(x=>x.addedNodes.length||x.removedNodes.length))schedule()});
  observer.observe(document.body,{childList:true,subtree:true});
  ['morley-valuation-history-updated','morley-watchlist-updated','online','offline','resize'].forEach(name=>window.addEventListener(name,schedule));
}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot,{once:true});else boot();
})();
