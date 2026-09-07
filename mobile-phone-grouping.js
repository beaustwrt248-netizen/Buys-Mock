(()=>{'use strict';
const $=(s,r=document)=>r.querySelector(s),$$=(s,r=document)=>[...r.querySelectorAll(s)];
const norm=s=>String(s||'').trim().replace(/\s+/g,' ');
const key=s=>norm(s).toLowerCase();
const storageFromCard=card=>{const p=$('p',card);if(!p)return'Unknown';const parts=norm(p.textContent).split('•').map(x=>norm(x));return parts[1]||'Unknown'};
const brandFromCard=card=>{const p=$('p',card);if(!p)return'';return norm(p.textContent).split('•').map(x=>norm(x))[0]||''};
const modelFromCard=card=>norm($('h3',card)?.textContent);
const priceNode=card=>$('.morley-chip',card);
function bestImage(cards){for(const card of cards){const img=$('img[data-morley-device-image],.morley-device-media img',card);if(img)return img}return null}
function groupGrid(){
 const grid=$('#morleyPhoneGrid');if(!grid||grid.dataset.groupingBusy==='1')return;
 const cards=$$('.morley-search-result',grid).filter(c=>!c.closest('.morley-group-source-bin'));
 if(!cards.length)return;
 grid.dataset.groupingBusy='1';
 try{
  const groups=new Map();
  for(const card of cards){const model=modelFromCard(card),brand=brandFromCard(card);if(!model)continue;const k=`${key(brand)}|${key(model)}`;if(!groups.has(k))groups.set(k,[]);groups.get(k).push(card)}
  const frag=document.createDocumentFragment();
  for(const cardsForModel of groups.values()){
   const first=cardsForModel[0],model=modelFromCard(first),brand=brandFromCard(first);
   const storageMap=new Map();
   for(const card of cardsForModel){const storage=storageFromCard(card),sk=key(storage);if(!storageMap.has(sk))storageMap.set(sk,{storage,cards:[]});storageMap.get(sk).cards.push(card)}
   const variants=[...storageMap.values()];
   const wrap=document.createElement('div');wrap.className='morley-phone-model-group';wrap.dataset.modelKey=`${key(brand)}|${key(model)}`;
   const media=$('.morley-device-media',first)?.cloneNode(true)||document.createElement('div');if(!media.classList.contains('morley-device-media'))media.className='morley-device-media is-placeholder';
   const sourceBin=document.createElement('div');sourceBin.className='morley-group-source-bin';cardsForModel.forEach(c=>sourceBin.appendChild(c));
   const copy=document.createElement('div');copy.className='morley-result-copy';
   const heading=document.createElement('h3');heading.textContent=model;
   const meta=document.createElement('p');meta.className='morley-group-meta';meta.textContent=[brand,'Mobile Phone'].filter(Boolean).join(' • ');
   const storages=document.createElement('div');storages.className='morley-storage-row';
   const price=document.createElement('span');price.className='morley-group-price';
   const actions=document.createElement('div');actions.className='morley-group-actions';
   const open=document.createElement('button');open.type='button';open.className='morley-group-open';open.textContent='Open Buy Flow';
   const fav=document.createElement('button');fav.type='button';fav.className='morley-group-favourite';fav.setAttribute('aria-label','Favourite selected storage');
   let selected=0;
   const sync=()=>{const entry=variants[selected]||variants[0],source=entry?.cards?.[0];$$('.morley-storage-chip',storages).forEach((b,i)=>b.classList.toggle('active',i===selected));const pn=source&&priceNode(source);price.textContent=pn?.textContent?.trim()||'Price to be added';price.classList.toggle('is-priced',!!pn?.classList.contains('morley-authorised'));fav.textContent=$('.morley-favourite',source)?.textContent?.trim()||'☆';const chosenMedia=$('.morley-device-media',source);if(chosenMedia&&chosenMedia.innerHTML!==media.innerHTML){media.className=chosenMedia.className;media.innerHTML=chosenMedia.innerHTML}}
   variants.forEach((entry,i)=>{const b=document.createElement('button');b.type='button';b.className='morley-storage-chip';b.textContent=entry.storage;b.dataset.duplicateCount=String(entry.cards.length);b.title=entry.cards.length>1?`${entry.cards.length} duplicate catalogue rows consolidated`:entry.storage;b.addEventListener('click',()=>{selected=i;sync()});storages.appendChild(b)});
   open.addEventListener('click',()=>{const source=variants[selected]?.cards?.[0];$('.morley-open-buy',source)?.click()});
   fav.addEventListener('click',()=>{const source=variants[selected]?.cards?.[0];$('.morley-favourite',source)?.click();setTimeout(sync,0)});
   copy.append(heading,meta,storages,price);actions.append(open,fav);wrap.append(media,copy,actions,sourceBin);sync();frag.appendChild(wrap)
  }
  grid.replaceChildren(frag);
  const count=$('#morleyPhoneCount');if(count){const modelCount=groups.size,variantCount=[...groups.values()].reduce((n,g)=>n+new Set(g.map(storageFromCard).map(key)).size,0);count.textContent=`${modelCount} model${modelCount===1?'':'s'} • ${variantCount} storage variant${variantCount===1?'':'s'}`}
 }finally{grid.dataset.groupingBusy='0'}
}
let queued=false;const schedule=()=>{if(queued)return;queued=true;requestAnimationFrame(()=>{queued=false;groupGrid()})};
function boot(){const grid=$('#morleyPhoneGrid');if(!grid)return setTimeout(boot,100);new MutationObserver(schedule).observe(grid,{childList:true});schedule()}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot,{once:true});else boot();
})();
