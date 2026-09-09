import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const gate=fs.readFileSync('nova/camera-valuation-safety.js','utf8');
const loader=fs.readFileSync('nova/live-support.js','utf8');

test('camera valuation safety loads before Camera Assistant',()=>{
  const safety=loader.indexOf("camera-valuation-safety.js?v=1");
  const camera=loader.indexOf("camera-assistant.js?v=2");
  assert.ok(safety>=0 && camera>safety);
});

test('exact live-catalogue model number can verify camera valuation identity',()=>{
  assert.match(gate,/norm\(row\?\.model_number\)===seenModel/);
  assert.match(gate,/exact model number matched the live catalogue/);
});

test('model-name fallback requires high confidence and one catalogue candidate',()=>{
  assert.match(gate,/confidence>=\.9/);
  assert.match(gate,/candidates\.length===1/);
});

test('ambiguous identity strips suggested maximum-buy guidance',()=>{
  assert.match(gate,/detail\.suggested_max_buy_aud=null/);
  assert.match(gate,/Human identity verification required/);
  assert.match(gate,/automatic maximum-buy guidance is withheld/);
});

test('storage conflict cannot qualify as a strong valuation identity',()=>{
  assert.match(gate,/storage conflicts with the catalogue variant/);
  assert.match(gate,/Math\.abs\(v-seen\)<\.5/);
});
