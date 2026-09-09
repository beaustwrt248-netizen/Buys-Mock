import test from 'node:test';
import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';

const src=()=>readFile(new URL('../admin/intelligence-command-centre.js',import.meta.url),'utf8');

test('Command Centre refreshes all available live evidence adapters',async()=>{
  const code=await src();
  for(const pattern of [/MorleyDeviceIntelligenceAdapter\?\.refresh/,/MorleyPricingIntelligenceAdapter\?\.refresh/,/refreshMorleySupportIntelligenceEvidence/,/refreshMorleyReleaseIntelligenceEvidence/,/MorleyMarketplaceLiveSource\?\.refresh/]) assert.match(code,pattern);
  assert.match(code,/filter\(fn=>typeof fn==='function'\)/);
  assert.match(code,/Promise\.allSettled\(jobs\)/);
});

test('Command Centre refresh preserves unavailable optional sources',async()=>{
  const code=await src();
  assert.match(code,/Promise\.resolve\(\)\.then\(\(\)=>fn\(\)\)/);
  assert.match(code,/finally/);
});

test('Australian catalogue metric uses explicit AU/Australia identity instead of loose au text',async()=>{
  const code=await src();
  assert.match(code,/region==='au'\|\|region\.includes\('australia'\)/);
  assert.match(code,/devices\.filter\(d=>isAustralianRegion\(d\.marketRegion\)\)/);
  assert.doesNotMatch(code,/australia\|australian\|au/);
});

test('support pressure comes from authoritative Support Intelligence rather than rendered rows',async()=>{
  const code=await src();
  assert.match(code,/snap\?\.sources\?\.support/);
  assert.match(code,/snap\?\.supportEvidence/);
  assert.match(code,/source:'Support Intelligence'/);
  assert.doesNotMatch(code,/safeCount\('#ticketsList/);
});
