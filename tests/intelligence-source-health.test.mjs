import test from 'node:test';
import assert from 'node:assert/strict';
import {createRequire} from 'node:module';
import {readFileSync} from 'node:fs';
import {fileURLToPath} from 'node:url';
import {dirname,resolve} from 'node:path';

const require=createRequire(import.meta.url);
const ROOT=resolve(dirname(fileURLToPath(import.meta.url)),'..');
const model=require(resolve(ROOT,'admin/intelligence-source-health-model.js'));

test('required intelligence sources fail closed when missing',()=>{
  const result=model.analyse({},Date.parse('2026-09-09T00:00:00Z'));
  assert.equal(result.state,'missing');
  for(const kind of ['catalogue','buyPricing','support','release'])assert.equal(result.rows.find(x=>x.kind===kind)?.state,'missing');
  for(const kind of ['pricing','marketplace'])assert.equal(result.rows.find(x=>x.kind===kind)?.state,'on_demand');
});

test('source health distinguishes live, stale and reference evidence',()=>{
  const now=Date.parse('2026-09-09T00:10:00Z');
  const result=model.analyse({
    catalogue:{checkedAt:'2026-09-09T00:09:00Z',source:'device_catalog',authoritative:true},
    buyPricing:{checkedAt:'2026-09-09T00:09:00Z',source:'admin-pricing-control',authoritative:true},
    support:{checkedAt:'2026-09-09T00:00:00Z',source:'support_tickets',authoritative:true},
    release:{checkedAt:'2026-09-09T00:09:00Z',source:'release evidence',authoritative:false},
    pricing:{checkedAt:'2026-09-09T00:09:00Z',source:'market-search-v2',authoritative:false}
  },now);
  assert.equal(result.rows.find(x=>x.kind==='catalogue')?.state,'live');
  assert.equal(result.rows.find(x=>x.kind==='support')?.state,'stale');
  assert.equal(result.rows.find(x=>x.kind==='release')?.state,'reference');
  assert.equal(result.rows.find(x=>x.kind==='pricing')?.state,'reference');
  assert.equal(result.state,'stale');
});

test('source health loader is wired through Smart Alerts and does not add write authority',()=>{
  const alerts=readFileSync(resolve(ROOT,'admin/smart-alerts.js'),'utf8');
  const centre=readFileSync(resolve(ROOT,'admin/intelligence-source-health.js'),'utf8');
  assert.match(alerts,/intelligence-source-health\.js\?v=1/);
  assert.match(centre,/MorleyDeviceIntelligenceAdapter\?\.refresh/);
  assert.match(centre,/MorleyPricingIntelligenceAdapter\?\.refresh/);
  assert.doesNotMatch(centre,/\.insert\(|\.update\(|\.delete\(|functions\.invoke\(/);
});
