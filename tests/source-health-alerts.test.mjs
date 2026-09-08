import test from 'node:test';
import assert from 'node:assert/strict';
import {createRequire} from 'node:module';
import {readFileSync} from 'node:fs';
import {fileURLToPath} from 'node:url';
import {dirname,resolve} from 'node:path';

const require=createRequire(import.meta.url);
const ROOT=resolve(dirname(fileURLToPath(import.meta.url)),'..');
const health=require(resolve(ROOT,'admin/intelligence-source-health-model.js'));
const alerts=require(resolve(ROOT,'admin/smart-alerts-model.js'));

test('required missing, stale and reference feeds become actionable Smart Alerts',()=>{
  const now=Date.parse('2026-09-09T00:10:00Z');
  const state=health.analyse({
    catalogue:{checkedAt:'2026-09-09T00:09:00Z',source:'device_catalog',authoritative:true},
    buyPricing:{checkedAt:'2026-09-09T00:09:00Z',source:'admin-pricing-control',authoritative:true},
    support:{checkedAt:'2026-09-09T00:00:00Z',source:'support_tickets',authoritative:true},
    release:{checkedAt:'2026-09-09T00:09:00Z',source:'release evidence',authoritative:false}
  },now);
  const rows=alerts.fromSourceHealth(state);
  assert.equal(rows.length,2);
  const support=rows.find(x=>x.detail.kind==='support');
  const release=rows.find(x=>x.detail.kind==='release');
  assert.equal(support.severity,'high');
  assert.equal(support.detail.state,'stale');
  assert.equal(release.severity,'medium');
  assert.equal(release.detail.state,'reference');
  assert.equal(alerts.normalize(support).action.target,'overview');
});

test('healthy and optional on-demand feeds do not create source alerts',()=>{
  const now=Date.parse('2026-09-09T00:10:00Z');
  const state=health.analyse({
    catalogue:{checkedAt:'2026-09-09T00:09:00Z',source:'device_catalog',authoritative:true},
    buyPricing:{checkedAt:'2026-09-09T00:09:00Z',source:'admin-pricing-control',authoritative:true},
    support:{checkedAt:'2026-09-09T00:09:30Z',source:'support_tickets',authoritative:true},
    release:{checkedAt:'2026-09-09T00:09:00Z',source:'release evidence',authoritative:true}
  },now);
  assert.equal(state.state,'healthy');
  assert.deepEqual(alerts.fromSourceHealth(state),[]);
});

test('Smart Alerts UI listens for source health changes and source health publishes only derived review evidence',()=>{
  const ui=readFileSync(resolve(ROOT,'admin/smart-alerts.js'),'utf8');
  const source=readFileSync(resolve(ROOT,'admin/intelligence-source-health.js'),'utf8');
  assert.match(ui,/morley:source-health-updated/);
  assert.match(ui,/model\.counts\.source/);
  assert.match(ui,/smart-alerts-model\.js\?v=2/);
  assert.match(source,/morley:source-health-updated/);
  assert.match(source,/totalAlerts/);
  assert.doesNotMatch(source,/\.insert\(|\.update\(|\.delete\(|functions\.invoke\(/);
});
