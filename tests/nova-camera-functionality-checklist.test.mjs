import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const checklist=fs.readFileSync('nova/camera-functionality-checklist.js','utf8');
const loader=fs.readFileSync('nova/live-support.js','utf8');

test('manual functionality checklist covers hidden device functions Nova must not infer visually',()=>{
  for(const label of ['Display & touch','Front & rear cameras','Charging & port','Face ID / fingerprint','Speakers & microphone','Buttons & switches','Wi-Fi / Bluetooth / cellular','Battery health / charging stability']) assert.ok(checklist.includes(label),`missing ${label}`);
  assert.match(checklist,/Visual AI does not infer hidden functionality/);
});

test('explicit functionality failure removes automatic maximum-buy value',()=>{
  assert.match(checklist,/if\(state\.failed\.length\)/);
  assert.match(checklist,/detail\.suggested_max_buy_aud=null/);
  assert.match(checklist,/Manual pricing required/);
});

test('untested functions remain a final-offer human gate',()=>{
  assert.match(checklist,/detail\.final_offer_ready=state\.complete&&!state\.failed\.length/);
  assert.match(checklist,/Functionality checks incomplete/);
});

test('checklist loads after Camera Assistant and before any later user interaction',()=>{
  const camera=loader.indexOf("camera-assistant.js?v=2");
  const checklistIndex=loader.indexOf("camera-functionality-checklist.js?v=1");
  assert.ok(camera>=0 && checklistIndex>camera);
});
