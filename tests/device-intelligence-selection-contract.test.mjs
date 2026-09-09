import test from 'node:test';
import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';

const intelligence=()=>readFile(new URL('../admin/device-intelligence-centre.js',import.meta.url),'utf8');
const testing=()=>readFile(new URL('../admin/device-testing-centre.js',import.meta.url),'utf8');

test('Device Intelligence publishes one canonical selection snapshot and event',async()=>{
  const code=await intelligence();
  assert.match(code,/window\.MorleyDeviceIntelligenceSelection=payload/);
  assert.match(code,/morley:device-intelligence-selection/);
  assert.match(code,/publishSelection\(null\)/);
  assert.match(code,/publishSelection\(d\)/);
  assert.match(code,/aria-pressed/);
});

test('Device Testing follows canonical selection rather than click-only DOM inference',async()=>{
  const code=await testing();
  assert.match(code,/MorleyDeviceIntelligenceSelection/);
  assert.match(code,/morley:device-intelligence-selection/);
  assert.match(code,/clearSession\(\)/);
  assert.doesNotMatch(code,/closest\?\.\('\.mdi-row'\)/);
  assert.doesNotMatch(code,/querySelector\('#mdiList \.mdi-row'\)/);
});
