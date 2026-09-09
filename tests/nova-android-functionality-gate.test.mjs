import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const gate=fs.readFileSync('android/novaapp/src/main/java/com/buysloans/nova/NovaVisionFunctionalityGate.java','utf8');
const init=fs.readFileSync('android/novaapp/src/main/java/com/buysloans/nova/NovaOperatorInitializer.java','utf8');

test('native Vision requires eight explicit manual hidden-function checks',()=>{
  for(const id of ['display_touch','front_rear_cameras','charging','biometrics','audio','buttons','connectivity','battery_health']) assert.ok(gate.includes(`"${id}"`));
  assert.match(gate,/Vision cannot prove hidden functionality/);
  assert.match(gate,/Status\.PENDING\?Status\.PASS:current==Status\.PASS\?Status\.FAIL:Status\.PENDING/);
});

test('failed functionality suppresses automatic max-buy while pending blocks final readiness',()=>{
  assert.match(gate,/manual pricing required because functionality failed/);
  assert.match(gate,/automatic max-buy suppressed/);
  assert.match(gate,/final offer not ready/);
  assert.match(gate,/human final approval still required/);
});

test('new analysis resets native functionality state',()=>{
  assert.match(gate,/value\.startsWith\("Analysing "\)/);
  assert.match(gate,/reset\(activity\)/);
});

test('Vision functionality installs only through Nova activity lifecycle and is cleared on destroy',()=>{
  assert.match(init,/activity instanceof NovaVisionActivity/);
  assert.match(init,/NovaVisionFunctionalityGate\.install/);
  assert.match(init,/NovaVisionFunctionalityGate\.clear/);
});


test('functionality state changes always re-render from the untouched pricing result',()=>{
  assert.match(gate,/String latestPricingResult/);
  assert.match(gate,/state\.latestPricingResult = withoutGateMarker\(value\)/);
  assert.match(gate,/String next=state\.latestPricingResult/);
  assert.doesNotMatch(gate,/String next=text/);
  assert.match(gate,/state\.latestPricingResult = null;\s*reset\(activity\)/);
});

test('gate-generated text cannot overwrite the retained authoritative pricing result',()=>{
  assert.match(gate,/if \(state\.rewriting\) return/);
  assert.match(gate,/private static String withoutGateMarker/);
  assert.match(gate,/return markerAt>=0\?text\.substring\(0,markerAt\):text/);
});
