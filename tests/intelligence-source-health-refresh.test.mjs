import test from 'node:test';
import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';

const src=()=>readFile(new URL('../admin/intelligence-source-health.js',import.meta.url),'utf8');

test('source health refresh includes the optional live marketplace source',async()=>{
  const code=await src();
  assert.match(code,/MorleyMarketplaceLiveSource\?\.refresh/);
  assert.match(code,/morley:marketplace-source-health/);
  assert.match(code,/Refreshing available live intelligence sources/);
});

test('source health refresh keeps optional sources non-fatal',async()=>{
  const code=await src();
  assert.match(code,/filter\(v=>v&&typeof v\.then==='function'\)/);
  assert.match(code,/Promise\.allSettled\(jobs\)/);
});
