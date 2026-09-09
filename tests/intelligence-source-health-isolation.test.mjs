import test from 'node:test';
import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';

const src=()=>readFile(new URL('../admin/intelligence-source-health.js',import.meta.url),'utf8');

test('Source Health defers every available refresher into its own promise',async()=>{
  const code=await src();
  assert.match(code,/calls\.filter\(fn=>typeof fn==='function'\)\.map\(fn=>Promise\.resolve\(\)\.then\(\(\)=>fn\(\)\)\)/);
  assert.match(code,/Promise\.allSettled\(jobs\)/);
});

test('Source Health reports partial refresh completion without suppressing other feeds',async()=>{
  const code=await src();
  assert.match(code,/settled\.filter\(x=>x\.status==='rejected'\)\.length/);
  assert.match(code,/sources checked successfully/);
  assert.match(code,/finally/);
});
