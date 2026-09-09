import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const activity=fs.readFileSync('android/novaapp/src/main/java/com/buysloans/nova/NovaVisionActivity.java','utf8');

test('native valuation allows market research but withholds max-buy until catalogue identity is strong',()=>{
  assert.match(activity,/formatValuation\(assessment, market, identityCheck\.verified\)/);
  assert.match(activity,/Suggested maximum buy: withheld until catalogue identity is verified/);
  assert.match(activity,/Market evidence can be researched from a plausible visual identity/);
});

test('exact model number can strongly verify native identity',()=>{
  assert.match(activity,/visualModelNumber\.equals\(normaliseIdentity\(row\.optString\("model_number"\)\)\)/);
  assert.match(activity,/exact model number matched the live catalogue/);
});

test('model-name fallback requires high confidence and exactly one candidate',()=>{
  assert.match(activity,/confidence >= \.90/);
  assert.match(activity,/if \(count == 1 && candidate != null\)/);
});

test('storage compatibility is required for strong identity',()=>{
  assert.match(activity,/storageCompatibleWithMatch\(assessment, row\)/);
  assert.match(activity,/storage conflicts with that catalogue variant/);
});

test('D and PARTS remain manual regardless of identity confidence',()=>{
  assert.match(activity,/manual pricing required for/);
  assert.match(activity,/"D"\.equals\(grade\) \|\| "PARTS"\.equals\(grade\)/);
});
