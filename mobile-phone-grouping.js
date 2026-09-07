(()=>{'use strict';
const $=(s,r=document)=>r.querySelector(s),$$=(s,r=document)=>[...r.querySelectorAll(s)];
const norm=s=>String(s||'').trim().replace(/\s+/g,' ');
const key=s=>norm(s).toLowerCase();
const modelFromCard=card=>norm($('h3',card)?.textContent);
const partsFromCard=card=>norm($('.morley-result-copy>p',card)?.textContent||$('p',card)?.textContent).split('•').map(norm).filter(Boolean);
const brandFromCard=card=>partsFromCard(card)[0]||'';
const capacity=(value,unit)=>`${value}${String(unit).toUpperCase()}`;
const storageCue='(?:internal\\s+storage|device\\s+storage|built[- ]?in\\s+storage|flash\\s+storage|storage|capacity|rom)';
const storageAfterRe=new RegExp(`\\b${storageCue}\\s*[:\\-=]?\\s*(\\d+(?:\\.\\d+)?)\\s*(TB|GB|MB)\\b`,'i');
const storageBeforeRe=new RegExp(`\\b(\\d+(?:\\.\\d+)?)\\s*(TB|GB|MB)\\s*${storageCue}\\b`,'i');
const storageFromPart=part=>{const text=norm(part);if(!text)return'';const after=text.match(storageAfterRe);if(after)return capacity(after[1],after[2]);const before=text.match(storageBeforeRe);if(before)return capacity(before[1],before[2]);if(/\b(?:ram|memory\s+ram|expandable|micro\s*sd|microsd|sd\s*card)\b/i.test(text))return'';const bare=text.match(/^\s*(\d+(?:\.\d+)?)\s*(TB|GB|MB)\s*$/i);return bare?capacity(bare[1],bare[2]):''};
const storageFromCard=card=>{for(const part of partsFromCard(card)){const storage=storageFromPart(part);if(storage)return storage}return''};
const priceNode=card=>$('.morley-chip',card);
const sourceFor=cards=>cards.find(c=>$('.morley-authorised',c))||cards[0];
const storageBytes=s=>{const m=String(s).match(/([\d.]+)(TB|GB|MB)/i);if(!m)return Number.MAX_SAFE_INTEGER;return Number(m[1])*({MB:1,GB:1024,TB:1048576}[m[2].toUpperCase()]||1)};
function prepareMedia(media){
 const img=$('img',media);if(!img){media.classList.add('is-placeholder');media.classList.remove('is-loaded');return}
 const src=img.currentSrc||img.getAttribute('src')||img.dataset.src||'';
 img.loading='lazy';img.decoding='async';img.removeAttribute('data-image-prepared');
 const loaded=()=>{media.classList.add('is-loaded');media.classList.remove('is-placeholder')};
 const failed=()=>{media.classList.remove('is-loaded');media.classList.add('is-placeholder')};
 img.addEventListener('load',loaded,{once:true});img.addEventListener('error',failed,{once:true});
 if(src&&!img.getAttribute('src'))img.src=src;
 if(img.complete&&img.naturalWidth>0)loaded();
}
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
   for(const card of cardsForModel){const storage=storageFromCard(card),sk=key(storage)||'__base__';if(!storageMap.has(sk))storageMap.set(sk,{storage,cards:[]});storageMap.get(sk).cards.push(card)}
   const variants=[...storageMap.values()].sort((a,b)=>storageBytes(a.storage)-storageBytes(b.storage));
   const wrap=document.createElement('article');wrap.className='morley-phone-model-group';wrap.dataset.modelKey=`${key(brand)}|${key(model)}`;
   const initialSource=sourceFor(variants[0]?.cards||cardsForModel);
   const media=$('.morley-device-media',initialSource)?.cloneNode(true)||document.createElement('div');if(!media.classList.contains('morley-device-media'))media.className='morley-device-media';prepareMedia(media);
   const sourceBin=document.createElement('div');sourceBin.className='morley-group-source-bin';cardsForModel.forEach(c=>sourceBin.appendChild(c));
   const copy=document.createElement('div');copy.className='morley-result-copy';
   const heading=document.createElement('h3');heading.textContent=model;
   const meta=document.createElement('p');meta.className='morley-group-meta';meta.textContent=[brand,'Mobile Phone'].filter(Boolean).join(' • ');
   const storages=document.createElement('div');storages.className='morley-storage-row';
   const price=document.createElement('span');price.className='morley-group-price';
   const actions=document.createElement('div');actions.className='morley-group-actions';
   const open=document.createElement('button');open.type='button';open.className='morley-group-open';open.textContent='Open Buy Flow';
   const fav=document.createElement('button');fav.type='button';fav.className='morley-group-favourite';
   let selected=Math.max(0,variants.findIndex(v=>v.storage));
   const sync=()=>{
    const entry=variants[selected]||variants[0],source=sourceFor(entry?.cards||[]);
    $$('.morley-storage-chip',storages).forEach(b=>{const active=Number(b.dataset.variantIndex)===selected;b.classList.toggle('active',active);b.setAttribute('aria-pressed',String(active))});
    const pn=source&&priceNode(source);price.textContent=pn?.textContent?.trim()||'Price to be added';price.classList.toggle('is-priced',!!pn?.classList.contains('morley-authorised'));
    fav.textContent=$('.morley-favourite',source)?.textContent?.trim()||'☆';fav.setAttribute('aria-label',`Favourite ${model}${entry?.storage?` ${entry.storage}`:''}`);
    const chosenMedia=$('.morley-device-media',source);if(chosenMedia){const fresh=chosenMedia.cloneNode(true);media.className=fresh.className;media.innerHTML=fresh.innerHTML;prepareMedia(media)}
   };
   variants.forEach((entry,i)=>{if(!entry.storage)return;const b=document.createElement('button');b.type='button';b.className='morley-storage-chip';b.textContent=entry.storage;b.dataset.variantIndex=String(i);b.dataset.duplicateCount=String(entry.cards.length);b.title=entry.cards.length>1?`${entry.cards.length} duplicate catalogue rows consolidated`:entry.storage;b.setAttribute('aria-label',`${model} ${entry.storage}`);b.addEventListener('click',()=>{selected=i;sync()});storages.appendChild(b)});
   if(!storages.childElementCount)storages.hidden=true;
   open.addEventListener('click',()=>{const source=sourceFor(variants[selected]?.cards||[]);$('.morley-open-buy',source)?.click()});
   fav.addEventListener('click',()=>{const source=sourceFor(variants[selected]?.cards||[]);$('.morley-favourite',source)?.click();setTimeout(sync,0)});
   copy.append(heading,meta,storages,price);actions.append(open,fav);wrap.append(media,copy,actions,sourceBin);sync();frag.appendChild(wrap)
  }
  grid.replaceChildren(frag);
 }finally{grid.dataset.groupingBusy='0'}
}
let queued=false;const schedule=()=>{if(queued)return;queued=true;requestAnimationFrame(()=>{queued=false;groupGrid()})};
function boot(){const grid=$('#morleyPhoneGrid');if(!grid)return setTimeout(boot,100);new MutationObserver(schedule).observe(grid,{childList:true});schedule()}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot,{once:true});else boot();
})();