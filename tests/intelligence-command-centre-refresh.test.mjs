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
