(()=>{'use strict';
const $=(s,r=document)=>r.querySelector(s),$$=(s,r=document)=>[...r.querySelectorAll(s)];
const norm=s=>String(s||'').trim().replace(/\s+/g,' '),key=s=>norm(s).toLowerCase();
const core=()=>window.MorleyCatalogueConnectivity;
function parts(card){return norm($('.morley-result-copy>p',card)?.textContent||$('p',card)?.textContent).split('•').map(norm).filter(Boolean)}
function categoryFromCard(card){const p=parts(card).map(key);if(p.includes('tablet'))return'tablet';if(p.includes('wearable'))return'wearable';return''}
function brandFromCard(card){return parts(card)[0]||''}
function storageFromCard(card){const p=parts(card),brand=key(brandFromCard(card));return p.find(x=>!['tablet','wearable'].includes(key(x))&&key(x)!==brand)||''}
function modelFromCard(card){return norm($('h3',card)?.textContent)}
function priceNode(card){return $('.morley-chip',card)}
function sourceFor(cards){return cards.find(c=>$('.morley-authorised',c))||cards[0]}
function prepareMedia(media){const img=$('img',media);if(!img){media.classList.add('is-placeholder');media.classList.remove('is-loaded');return}img.loading='lazy';img.decoding='async';const loaded=()=>{media.classList.add('is-loaded');media.classList.remove('is-placeholder')};const failed=()=>{media.classList.remove('is-loaded');media.classList.add('is-placeholder')};img.addEventListener('load',loaded,{once:true});img.addEventListener('error',failed,{once:true});if(img.complete&&img.naturalWidth>0)loaded()}
function choiceButton(label,kind,onClick){const b=document.createElement('button');b.type='button';b.className='morley-variant-chip';b.dataset.choiceKind=kind;b.dataset.choiceValue=label;b.textContent=label;b.addEventListener('click',onClick);return b}
function unique(values){return[...new Set(values.filter(Boolean))]}
function groupedCard(cards){
 const first=cards[0],category=categoryFromCard(first),brand=brandFromCard(first),parsed=core().parseVariant(category,modelFromCard(first)),baseModel=parsed.baseModel;
 const variants=cards.map(card=>{const info=core().parseVariant(category,modelFromCard(card));return{card,storage:storageFromCard(card),connectivity:info.connectivity,info}});
 let selected=variants.find(v=>v.storage||v.connectivity)||variants[0];
 const wrap=document.createElement('article');wrap.className='morley-catalogue-model-group';wrap.dataset.category=category;wrap.dataset.modelKey=core().groupKey({category,brand,model:modelFromCard(first)});
 const initial=sourceFor(cards);const media=$('.morley-device-media',initial)?.cloneNode(true)||document.createElement('div');if(!media.classList.contains('morley-device-media'))media.className='morley-device-media';prepareMedia(media);
 const copy=document.createElement('div');copy.className='morley-result-copy';const heading=document.createElement('h3');heading.textContent=baseModel;const meta=document.createElement('p');meta.className='morley-group-meta';meta.textContent=[brand,category==='tablet'?'Tablet':'Smartwatch'].filter(Boolean).join(' • ');
 const storageField=document.createElement('div');storageField.className='morley-variant-field';const storageLabel=document.createElement('span');storageLabel.className='morley-variant-label';storageLabel.textContent='Storage';const storageRow=document.createElement('div');storageRow.className='morley-variant-row';storageField.append(storageLabel,storageRow);
 const connectivityField=document.createElement('div');connectivityField.className='morley-variant-field';const connectivityLabel=document.createElement('span');connectivityLabel.className='morley-variant-label';connectivityLabel.textContent='Connectivity';const connectivityRow=document.createElement('div');connectivityRow.className='morley-variant-row';connectivityField.append(connectivityLabel,connectivityRow);
 const price=document.createElement('span');price.className='morley-group-price';
 const actions=document.createElement('div');actions.className='morley-catalogue-group-actions';const open=document.createElement('button');open.type='button';open.className='morley-group-open';open.textContent='Open Buy Flow';const fav=document.createElement('button');fav.type='button';fav.className='morley-group-favourite';actions.append(open,fav);
 const sourceBin=document.createElement('div');sourceBin.className='morley-group-source-bin';cards.forEach(card=>sourceBin.appendChild(card));
 const storages=unique(variants.map(v=>v.storage));const connectivities=unique(variants.map(v=>v.connectivity));
 const selectStorage=value=>{const exact=variants.find(v=>v.storage===value&&v.connectivity===selected.connectivity);selected=exact||variants.find(v=>v.storage===value)||selected;sync()};
 const selectConnectivity=value=>{const exact=variants.find(v=>v.connectivity===value&&v.storage===selected.storage);selected=exact||variants.find(v=>v.connectivity===value)||selected;sync()};
 storages.forEach(value=>storageRow.appendChild(choiceButton(value,'storage',()=>selectStorage(value))));connectivities.forEach(value=>connectivityRow.appendChild(choiceButton(value,'connectivity',()=>selectConnectivity(value))));
 if(!storages.length)storageField.hidden=true;if(!connectivities.length)connectivityField.hidden=true;
 function sync(){
  $$('.morley-variant-chip',storageRow).forEach(b=>{const active=b.dataset.choiceValue===selected.storage;b.classList.toggle('active',active);b.setAttribute('aria-pressed',String(active))});
  $$('.morley-variant-chip',connectivityRow).forEach(b=>{const active=b.dataset.choiceValue===selected.connectivity;b.classList.toggle('active',active);b.setAttribute('aria-pressed',String(active))});
  const source=selected.card,pn=priceNode(source);price.textContent=pn?.textContent?.trim()||'Price to be added';price.classList.toggle('is-priced',!!pn?.classList.contains('morley-authorised'));fav.textContent=$('.morley-favourite',source)?.textContent?.trim()||'☆';fav.setAttribute('aria-label',`Favourite ${baseModel}${selected.storage?` ${selected.storage}`:''}${selected.connectivity?` ${selected.connectivity}`:''}`);
  const chosenMedia=$('.morley-device-media',source);if(chosenMedia){const fresh=chosenMedia.cloneNode(true);media.className=fresh.className;media.innerHTML=fresh.innerHTML;prepareMedia(media)}
 }
 open.addEventListener('click',()=>$('.morley-open-buy',selected.card)?.click());fav.addEventListener('click',()=>{$('.morley-favourite',selected.card)?.click();setTimeout(sync,0)});
 copy.append(heading,meta,storageField,connectivityField,price);wrap.append(media,copy,actions,sourceBin);sync();return wrap;
}
function groupUniversal(){const root=$('#morleyUniversalResults');if(!root||root.dataset.connectivityGroupingBusy==='1'||!core())return;const direct=$$(':scope > .morley-search-result',root).filter(card=>categoryFromCard(card));if(!direct.length)return;root.dataset.connectivityGroupingBusy='1';try{const all=$$(':scope > .morley-search-result',root),groups=new Map();for(const card of direct){const category=categoryFromCard(card),brand=brandFromCard(card),model=modelFromCard(card),k=core().groupKey({category,brand,model});if(!groups.has(k))groups.set(k,[]);groups.get(k).push(card)}const emitted=new Set(),frag=document.createDocumentFragment();for(const card of all){const category=categoryFromCard(card);if(!category){frag.appendChild(card);continue}const k=core().groupKey({category,brand:brandFromCard(card),model:modelFromCard(card)});if(emitted.has(k))continue;emitted.add(k);frag.appendChild(groupedCard(groups.get(k)))}const count=document.createElement('div');count.className='morley-grouped-result-count';count.textContent=`${emitted.size} grouped tablet/smartwatch device${emitted.size===1?'':'s'} in these results`;if(emitted.size)frag.prepend(count);root.replaceChildren(frag)}finally{root.dataset.connectivityGroupingBusy='0'}}
function updatePhoneCount(){const grid=$('#morleyPhoneGrid'),count=$('#morleyPhoneCount');if(!grid||!count)return;const groups=$$(':scope > .morley-phone-model-group',grid);if(groups.length)count.textContent=`${groups.length} catalogue device${groups.length===1?'':'s'}`}
let queued=false;function schedule(){if(queued)return;queued=true;requestAnimationFrame(()=>{queued=false;groupUniversal();updatePhoneCount()})}
function boot(){const universal=$('#morleyUniversalResults'),phones=$('#morleyPhoneGrid');if(!universal&&!phones)return setTimeout(boot,120);if(universal)new MutationObserver(schedule).observe(universal,{childList:true});if(phones)new MutationObserver(schedule).observe(phones,{childList:true});schedule()}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot,{once:true});else boot();
})();
