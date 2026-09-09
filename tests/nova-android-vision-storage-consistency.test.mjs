import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const activity=fs.readFileSync('android/novaapp/src/main/java/com/buysloans/nova/NovaVisionActivity.java','utf8');

test('Android Vision normalises GB and TB storage evidence before catalogue comparison',()=>{
  assert.match(activity,/Pattern\.compile\("\(\\\\d\+\(\?:\\\\\.\\\\d\+\)\?\)\\\\s\*\(TB\|GB\)"/);
  assert.match(activity,/"TB"\.equalsIgnoreCase\(matcher\.group\(2\)\) \? value \* 1024d : value/);
});

test('unknown storage evidence never becomes a conflict',()=>{
  assert.match(activity,/storage is unknown or not reliably visible/);
  assert.match(activity,/catalogue storage options are unavailable, so no conflict is inferred/);
});

test('supported catalogue storage keeps valuation available and explicit mismatches block it',()=>{
  assert.match(activity,/is supported by the matched catalogue model/);
  assert.match(activity,/Storage conflict: Vision inferred/);
  assert.match(activity,/valuation\.setEnabled\(hasPricingIdentity\(assessment\) && !storageCheck\.conflict\)/);
  assert.match(activity,/if \(storageCheck\.conflict\)/);
  assert.match(activity,/Valuation blocked:/);
});
