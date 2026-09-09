import test from 'node:test';
import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';

const src=()=>readFile(new URL('../admin/smart-alerts.js',import.meta.url),'utf8');

test('Smart Alerts refreshes support and release evidence it displays',async()=>{
  const code=await src();
  assert.match(code,/refreshMorleySupportIntelligenceEvidence/);
  assert.match(code,/refreshMorleyReleaseIntelligenceEvidence/);
  assert.match(code,/MorleyDeviceIntelligenceAdapter\?\.refresh/);
  assert.match(code,/MorleyPricingIntelligenceAdapter\?\.refresh/);
});

test('Smart Alerts isolates refresh failures and restores the refresh control',async()=>{
  const code=await src();
  assert.match(code,/Promise\.allSettled/);
  assert.match(code,/Promise\.resolve\(\)\.then\(run\)/);
  assert.match(code,/finally/);
  assert.match(code,/Refresh sources/);
});
