import test from 'node:test';
import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';

const src=()=>readFile(new URL('../admin/smart-alerts.js',import.meta.url),'utf8');

test('Smart Alerts refresh invokes available live evidence adapters before rendering',async()=>{
  const code=await src();
  assert.match(code,/MorleyDeviceIntelligenceAdapter\?\.refresh/);
  assert.match(code,/MorleyPricingIntelligenceAdapter\?\.refresh/);
  assert.match(code,/MorleyMarketplaceLiveSource\?\.refresh/);
  assert.match(code,/Promise\.allSettled/);
  assert.match(code,/refresh\.onclick=refreshEvidence/);
});

test('Smart Alerts refresh remains safe when optional adapters are unavailable',async()=>{
  const code=await src();
  assert.match(code,/if\(typeof fn==='function'\)/);
  assert.match(code,/if\(tasks\.length\)/);
  assert.match(code,/finally/);
});
