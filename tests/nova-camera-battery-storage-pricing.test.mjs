import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const source=fs.readFileSync('nova/camera-battery-storage-pricing.js','utf8');
const loader=fs.readFileSync('nova/live-support.js','utf8');

test('camera assessment outcome exposes storage and battery-health inputs',()=>{
  assert.match(source,/cameraStorageOverride/);
  assert.match(source,/cameraBatteryHealth/);
  assert.match(source,/Storage/);
  assert.match(source,/Battery health/);
});

test('camera pricing reuses the central Morley valuation rules',()=>{
  assert.match(source,/MorleyAssessmentCore/);
  assert.match(source,/core\?\.buildValuationQuote/);
  assert.match(source,/batteryHealthPct/);
  assert.match(source,/marketBaselineStorageSpecific:true/);
  assert.match(source,/storageAdjustment:0/);
});

test('storage must be verified before adjusted suggested pricing is exposed',()=>{
  assert.match(source,/Storage required for suggested pricing/);
  assert.match(source,/detail\.suggested_max_buy_aud=null/);
});

test('battery/storage adjustment never bypasses the strong identity safety gate',()=>{
  assert.match(source,/detail\?\.identity_gate\?\.verified!==true/);
});

test('adjusted assessment outcome publishes pricing context for audit consumers',()=>{
  assert.match(source,/detail\.battery_health_pct=/);
  assert.match(source,/detail\.battery_health_adjustment_aud=/);
  assert.match(source,/detail\.adjusted_market_value_aud=/);
  assert.match(source,/detail\.storage=/);
});

test('loader initialises central valuation core and battery-storage module in order',()=>{
  const core=loader.indexOf("../morley-ai-assessment-core.js?v=2");
  const camera=loader.indexOf("camera-assistant.js?v=2");
  const context=loader.indexOf("camera-battery-storage-pricing.js?v=1");
  const protectedBase=loader.indexOf("camera-approved-base-price.js?v=1");
  assert.ok(core>=0 && camera>core && context>camera && protectedBase>context);
});