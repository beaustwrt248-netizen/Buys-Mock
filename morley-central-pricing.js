(()=>{'use strict';
const STORE='morley-central-pricing-v2',HISTORY='morley-central-pricing-history-v1',META='morley-central-pricing-meta-v2',AUTH='morley_web_auth';
const SUPABASE_URL='https://ghdhairijqjqivqriigi.supabase.co',API_KEY='sb_publishable_ch49o8WRnDb8pPzowZH3Tg_XZcIbgvt';
const CATALOGUE_API=`${SUPABASE_URL}/functions/v1/app-pricing-catalogue`;
const normalizeStorage=v=>String(v||'').trim().replace(/\s+/g,'').toUpperCase();
const mapCategory=v=>v==='mobile_phone'?'phone':String(v||'').trim();
function load(){try{const v=JSON.parse(localStorage.getItem(STORE)||'[]');return Array.isArray(v)?v:[]}catch{return[]}}
function save(rows,source='cache'){localStorage.setItem(STORE,JSON.stringify(rows));localStorage.setItem(META,JSON.stringify({source,syncedAt:new Date().toISOString(),count:rows.length}));dispatchEvent(new CustomEvent('morley-pricing-updated',{detail:{rows,source}}));}
function history(){try{return JSON.parse(localStorage.getItem(HISTORY)||'[]')}catch{return[]}}
function updatePrice(id,value,actor='admin'){const rows=load();const row=rows.find(x=>x.id===id);if(!row)throw new Error('Unknown pricing row');if(!Number.isFinite(+value)||+value<0)throw new Error('Invalid price');const oldValue=row.value;row.value=+value;row.updatedAt=new Date().toISOString();row.updatedBy=actor;const h=history();h.unshift({id,oldValue,newValue:+value,actor,at:row.updatedAt});localStorage.setItem(HISTORY,JSON.stringify(h.slice(0,1000)));save(rows,'local');return row}
function search(q){const t=String(q||'').trim().toLowerCase();if(!t)return[];return load().filter(r=>[r.brand,r.model,r.storage,r.modelNumber,r.id].join(' ').toLowerCase().includes(t));}
function get(id){return load().find(r=>r.id===id)||null}
function reset(){localStorage.removeItem(STORE);localStorage.removeItem(META);dispatchEvent(new CustomEvent('morley-pricing-updated',{detail:{rows:[],source:'cleared'}}));}
function authSession(){try{return JSON.parse(localStorage.getItem(AUTH)||'null')}catch{return null}}
function status(){try{return JSON.parse(localStorage.getItem(META)||'null')||{source:'live-device-catalogue',syncedAt:null,count:load().length}}catch{return{source:'live-device-catalogue',syncedAt:null,count:load().length}}}
function catalogueRows(payload){
 const devices=Array.isArray(payload?.devices)?payload.devices:[],prices=Array.isArray(payload?.prices)?payload.prices:[];
 const priceByDevice=new Map();
 for(const price of prices){if(price?.authoritative!==true||!Number.isFinite(Number(price?.price_aud)))continue;const id=Number(price.device_catalog_id);if(!Number.isFinite(id))continue;const key=`${id}|${normalizeStorage(price.storage)}`;priceByDevice.set(key,price)}
 const rows=[];
 for(const device of devices){
  const id=Number(device?.id);if(!Number.isFinite(id)||id<=0)continue;
  const storageSet=new Map();
  for(const storage of Array.isArray(device.storage_options)?device.storage_options:[]){const clean=String(storage||'').trim();if(clean)storageSet.set(normalizeStorage(clean),clean)}
  for(const price of prices){if(Number(price?.device_catalog_id)!==id)continue;const clean=String(price?.storage||'').trim();if(clean&&!storageSet.has(normalizeStorage(clean)))storageSet.set(normalizeStorage(clean),clean)}
  if(!storageSet.size)storageSet.set('','');
  for(const [storageKey,storage] of storageSet){
   const price=priceByDevice.get(`${id}|${storageKey}`)||null;
   rows.push({
    id:`device:${id}:${storageKey||'default'}`,
    deviceCatalogId:id,
    category:mapCategory(device.category),
    brand:String(device.brand||''),
    model:String(device.model_name||''),
    modelNumber:String(device.model_number||''),
    storage,
    value:price?Number(price.price_aud):null,
    authoritative:price?.authoritative===true,
    source:price?'Morley approved price':'Live device catalogue',
    updatedAt:null
   });
  }
 }
 return rows.sort((a,b)=>String(a.category).localeCompare(String(b.category))||a.brand.localeCompare(b.brand)||a.model.localeCompare(b.model)||a.storage.localeCompare(b.storage));
}
async function sync(){const session=authSession();const token=session?.access_token;if(!token)return{ok:false,reason:'not-authenticated',...status()};try{const r=await fetch(CATALOGUE_API,{method:'GET',headers:{apikey:API_KEY,Authorization:`Bearer ${token}`,'Cache-Control':'no-cache'}});let data={};try{data=await r.json()}catch{}if(!r.ok)throw new Error(data?.error||`Catalogue sync failed (${r.status})`);const rows=catalogueRows(data);save(rows,'shared-device-catalogue');return{ok:true,...status()}}catch(error){dispatchEvent(new CustomEvent('morley-pricing-sync-failed',{detail:{message:String(error?.message||error)}}));return{ok:false,reason:'network',error:String(error?.message||error),...status()}}}
window.MorleyCentralPricing={version:3,load,search,get,updatePrice,history,reset,sync,status,get extraBrands(){return[]},grades:{A:.70,B:.50,C:.30},money:v=>new Intl.NumberFormat('en-AU',{style:'currency',currency:'AUD',maximumFractionDigits:0}).format(Number(v)||0)};
const kick=()=>sync();if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',kick,{once:true});else kick();addEventListener('online',kick);
})();