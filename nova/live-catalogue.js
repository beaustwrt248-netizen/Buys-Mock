(()=>{'use strict';
const PAGE_SIZE=1000;
const LABELS={console:'Consoles',desktop:'Desktops',laptop:'Laptops',mobile_phone:'Mobile Phones',tablet:'Tablets',wearable:'Wearables'};
const categories=()=>Object.keys(LABELS).map(category=>({category,label:LABELS[category],total:0,active:0,missing_model_number:0,missing_storage:0,missing_release_year:0,missing_image:0,au_region:0}));
const missing=value=>value==null||(typeof value==='string'&&!value.trim())||(Array.isArray(value)&&value.length===0);
const australian=value=>/(^|[^a-z])au([^a-z]|$)|australia/i.test(String(value||''));
const nonVariantStorageConsole=/\b(game boy(?: advance(?: sp)?| color| micro| pocket)?|new nintendo (?:2ds|3ds)(?: xl)?|nintendo (?:2ds|3ds)(?: xl)?|nintendo ds(?: lite)?|nintendo dsi(?: xl)?|nintendo 64|nintendo entertainment system|nintendo gamecube|super nintendo entertainment system|playstation 2(?: slim)?|playstation classic|playstation portal remote player|ps ?one)\b/i;
function storageExpected(item){if(String(item?.category||'')==='wearable')return false;if(String(item?.category||'')==='console'){const name=String(item?.model_name||'').trim();if(/^playstation$/i.test(name)||nonVariantStorageConsole.test(`${item?.brand||''} ${item?.family||''} ${name}`))return false}return true}
async function rows(){
  if(!window.NovaAuth?.rest)throw new Error('Nova authentication is required for live catalogue health.');
  const all=[];
  for(let offset=0;;offset+=PAGE_SIZE){
    const page=await window.NovaAuth.rest('/rest/v1/device_catalog?select=id,category,brand,family,model_name,model_number,storage_options,release_year,image_reference_url,active,market_region&order=id.asc&limit='+PAGE_SIZE+'&offset='+offset);
    if(!Array.isArray(page))throw new Error('Invalid live catalogue response.');
    all.push(...page);
    if(page.length<PAGE_SIZE)return all;
  }
}
async function load(){
  const all=await rows(), byCategory=new Map(categories().map(row=>[row.category,row]));
  let active=0,au=0,modelGaps=0,storageGaps=0,yearGaps=0,imageGaps=0;
  for(const item of all){
    const group=byCategory.get(item.category);
    if(!group)continue;
    group.total++;
    if(item.active!==true)continue;
    active++;group.active++;
    if(missing(item.model_number)){modelGaps++;group.missing_model_number++}
    if(storageExpected(item)&&missing(item.storage_options)){storageGaps++;group.missing_storage++}
    if(missing(item.release_year)){yearGaps++;group.missing_release_year++}
    if(missing(item.image_reference_url)){imageGaps++;group.missing_image++}
    if(australian(item.market_region)){au++;group.au_region++}
  }
  const now=new Date();
  return{schema_version:4,source:'Supabase public.device_catalog live authenticated read',checked_date:now.toISOString().slice(0,10),checked_at:now.toISOString(),snapshot_type:'live_query',privacy:'catalogue identity and quality fields only; no protected pricing',total_records:all.length,active_records:active,au_region_records:au,missing_model_number:modelGaps,missing_storage_actionable:storageGaps,missing_release_year:yearGaps,missing_image:imageGaps,categories:[...byCategory.values()]};
}
window.NovaCatalogueLive={load,storageExpected};
})();
