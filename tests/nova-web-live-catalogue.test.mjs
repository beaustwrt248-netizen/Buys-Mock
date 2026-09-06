import assert from 'node:assert/strict';
import fs from 'node:fs';
import test from 'node:test';
import vm from 'node:vm';

const live=fs.readFileSync(new URL('../nova/live-catalogue.js',import.meta.url),'utf8');
const core=fs.readFileSync(new URL('../nova/app-core.js',import.meta.url),'utf8');
const conversation=fs.readFileSync(new URL('../nova/conversation.js',import.meta.url),'utf8');
const recommendations=fs.readFileSync(new URL('../nova/recommendations.js',import.meta.url),'utf8');
const auth=fs.readFileSync(new URL('../nova/app.js',import.meta.url),'utf8');
const html=fs.readFileSync(new URL('../nova/index.html',import.meta.url),'utf8');

test('Nova web catalogue health is authenticated, live, complete and price-free',()=>{
  assert.match(html,/live-catalogue\.js\?v=1/);
  assert.match(auth,/rest:async path/);
  assert.match(auth,/cache:'no-store'/);
  assert.match(live,/window\.NovaAuth\?\.rest/);
  assert.match(live,/offset\+=PAGE_SIZE/);
  assert.match(live,/if\(page\.length<PAGE_SIZE\)return all/);
  assert.match(live,/select=id,category,model_number,storage_options,active,market_region/);
  assert.doesNotMatch(live,/buy_price|sell_price|device_buy_prices/);
  assert.match(live,/snapshot_type:'live_query'/);
});

test('every web catalogue consumer uses the live loader',()=>{
  assert.match(core,/window\.NovaCatalogueLive\.load\(\)/);
  assert.match(conversation,/window\.NovaCatalogueLive\.load\(\)/);
  assert.match(conversation,/loadEvidence\(intent==='catalogue'\)/);
  assert.match(recommendations,/window\.NovaCatalogueLive\.load\(\)/);
  assert.doesNotMatch(core,/localJson\('catalogue-health\.json'\)/);
  assert.doesNotMatch(conversation,/local\('catalogue-health\.json'\)/);
  assert.doesNotMatch(recommendations,/local\('catalogue-health\.json'\)/);
});

test('live loader aggregates beyond the first 1,000 rows',async()=>{
  const rows=Array.from({length:1481},(_,index)=>({
    id:index+1,
    category:index<470?'mobile_phone':'tablet',
    model_number:index===1200?'':`MODEL-${index+1}`,
    storage_options:index===1300?[]:['128GB'],
    active:index>=1364?false:true,
    market_region:index<979?'AU':'Global'
  }));
  const offsets=[];
  const context={window:{NovaAuth:{rest:async path=>{
    const offset=Number(new URL('https://nova.invalid'+path).searchParams.get('offset'));
    offsets.push(offset);
    return rows.slice(offset,offset+1000);
  }}},Date};
  vm.runInNewContext(live,context);

  const result=await context.window.NovaCatalogueLive.load();
  assert.deepEqual(offsets,[0,1000]);
  assert.equal(result.total_records,1481);
  assert.equal(result.active_records,1364);
  assert.equal(result.au_region_records,979);
  assert.equal(result.missing_model_number,1);
  assert.equal(result.missing_storage_actionable,1);
});
