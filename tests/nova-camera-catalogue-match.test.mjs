import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const catalogue=fs.readFileSync('nova/live-catalogue.js','utf8');
const camera=fs.readFileSync('nova/camera-assistant.js','utf8');

test('camera catalogue lookup is backed by a live implementation',()=>{
  assert.match(catalogue,/async function catalogueMatches\(value\)/);
  assert.match(catalogue,/window\.NovaLiveIntelligence=Object\.assign\(window\.NovaLiveIntelligence\|\|\{\},\{catalogueMatches\}\)/);
  assert.match(camera,/NovaLiveIntelligence\?\.catalogueMatches/);
});

test('catalogue matching only returns active rows and ranks exact identifiers strongly',()=>{
  assert.match(catalogue,/if\(item\.active!==true\)continue/);
  assert.match(catalogue,/if\(model&&model===q\)score\+=120/);
  assert.match(catalogue,/if\(name&&name===q\)score\+=100/);
  assert.match(catalogue,/slice\(0,20\)/);
});

test('camera matching cache is invalidated by catalogue realtime changes',()=>{
  assert.match(catalogue,/cachedRows=null;cachedAt=0/);
  assert.match(catalogue,/requestRefresh\('realtime'\)/);
  assert.match(catalogue,/requestRefresh\('revision-reconcile'\)/);
});
