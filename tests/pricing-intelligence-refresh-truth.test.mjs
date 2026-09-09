import test from 'node:test';
import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';

const src=()=>readFile(new URL('../admin/pricing-intelligence-centre.js',import.meta.url),'utf8');

test('Pricing Intelligence does not label a cached recompute as a live refresh',async()=>{
  const code=await src();
  assert.match(code,/Recompute loaded evidence/);
  assert.match(code,/Run a live market search from Device Intelligence/);
  assert.doesNotMatch(code,/>Refresh intelligence</);
});

test('Pricing Intelligence still renders only loaded runtime evidence',async()=>{
  const code=await src();
  assert.match(code,/MorleyIntelligenceRuntime\.snapshot\(\)/);
  assert.match(code,/mpiRefresh.*\.onclick=render/s);
});
