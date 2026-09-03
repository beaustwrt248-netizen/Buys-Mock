(()=>{
'use strict';
const $=(s,r=document)=>r.querySelector(s),$$=(s,r=document)=>[...r.querySelectorAll(s)];
let queued=false,observer=null;
const SPELLING=new Map([['ANALYZE LAPTOP','Analyse Laptop'],['ANALYZE DESKTOP','Analyse Desktop'],['ANALYZE NOW','Analyse Now'],['Analyze Laptop','Analyse Laptop'],['Analyze Desktop','Analyse Desktop'],['Analyze Now','Analyse Now'],['Seller asking price','Seller Ask'],['SELLER ASKING PRICE','Seller Ask'],['Reccomended Offer','Recommended Offer'],['Recommened Offer','Recommended Offer'],['Comparibles','Comparables'],['Valutation','Valuation'],['Availible','Available']]);
const STATUS_LABELS=new Set(['LIVE PRICING','ONLINE STATUS']);
const DECISION_LABELS=new Set(['ADJUSTED RESALE','RECOMMENDED OFFER','ABSOLUTE MAX BUY','EXPECTED PROFIT']);
const cleanText=s=>String(s||'').trim().replace(/\s+/g,' ');
function ensureViewport(){let meta=$('meta[name="viewport"]');if(!meta){meta=document.createElement('meta');meta.name='viewport';document.head.appendChild(meta)}meta.content='width=device-width,initial-scale=1,viewport-fit=cover'}
function normaliseSpelling(){const walker=document.createTreeWalker(document.body,NodeFilter.SHOW_TEXT,{acceptNode(node){const p=node.parentElement;if(!p||p.closest('script,style,textarea,input,select,option,code,pre'))return NodeFilter.FILTER_REJECT;return cleanText(node.nodeValue)?NodeFilter.FILTER_ACCEPT:NodeFilter.FILTER_REJECT}});const nodes=[];while(walker.nextNode())nodes.push(walker.currentNode);for(const node of nodes){const raw=cleanText(node.nodeValue),replacement=SPELLING.get(raw);if(replacement)node.nodeValue=node.nodeValue.replace(raw,replacement)}}
function markStatus(){for(const el of $$('small,label,b,strong,span,div')){const text=cleanText(el.textContent).toUpperCase();if(!STATUS_LABELS.has(text))continue;el.classList.add('morley-ui-status-label');const card=el.closest('.card,.stats>div,.tile,.metric,[class*="card"],[class*="tile"]')||el.parentElement;if(!card)continue;card.classList.add('morley-ui-status-card');[...card.children].forEach(child=>{if(child!==el)child.classList.add('morley-ui-status-value')})}}
function markDecision(){for(const el of $$('h1,h2,h3,h4,b,strong,span')){const text=cleanText(el.textContent).toUpperCase();if(text==='BUYING DECISION'){const card=el.closest('.card,.result,[class*="card"],[class*="result"]')||el.parentElement;card?.classList.add('morley-ui-buying-decision')}if(DECISION_LABELS.has(text)){const metric=el.closest('.decisionGrid>div,.decisionGrid>section,.decisionGrid>article')||el.parentElement;metric?.classList.add('morley-ui-decision-metric')}if(['HOLD','BUY','NEGOTIATE','PASS','REJECT'].includes(text)){const panel=el.closest('.notice,.decision,.verdict,[class*="verdict"],[class*="decision"]')||el.parentElement;if(panel&&!panel.classList.contains('decisionGrid'))panel.classList.add('morley-ui-verdict-panel')}}}
function canonicalLabels(){
  const title=$('.top>b,.top>strong');
  if(title&&/b&l morley/i.test(title.textContent||''))title.textContent='B&L Morley';
  const navLabels={home:'Home',laptop:'Categories',general:'General Buys',settings:'More',more:'More',computer:'Computer',console:'Console'};
  $$('nav button[data-page]').forEach(button=>{
    const label=navLabels[button.dataset.page];
    if(!label)return;
    const small=$('small',button);
    if(small){if(cleanText(small.textContent)!==label)small.textContent=label;return;}
    if(cleanText(button.textContent)!==label)button.textContent=label;
  });
}
function apply(){ensureViewport();normaliseSpelling();markStatus();markDecision();canonicalLabels();document.documentElement.dataset.morleyUiBaseline='1'}
function schedule(){if(queued)return;queued=true;(window.requestAnimationFrame||window.setTimeout)(()=>{queued=false;apply()})}
function boot(){apply();observer=new MutationObserver(schedule);observer.observe(document.body,{childList:true,subtree:true,characterData:true});window.addEventListener('resize',schedule,{passive:true});window.addEventListener('morley-product-parity-ready',schedule);window.addEventListener('morley-initial-layout-ready',schedule)}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot,{once:true});else boot();
})();

(()=>{
'use strict';
function loadParity(){
  if(!document.querySelector('link[data-morley-apk-home-parity]')){
    const link=document.createElement('link');
    link.rel='stylesheet';
    link.href='web-apk-home-parity.css?v=2';
    link.dataset.morleyApkHomeParity='1';
    document.head.appendChild(link);
  }
  if(!document.querySelector('script[data-morley-apk-home-parity]')){
    const script=document.createElement('script');
    script.src='web-apk-home-parity.js?v=2';
    script.defer=true;
    script.dataset.morleyApkHomeParity='1';
    document.body.appendChild(script);
  }
}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',loadParity,{once:true});else loadParity();
})();
