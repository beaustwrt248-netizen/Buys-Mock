import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const provenance=fs.readFileSync('nova/camera-photo-provenance.js','utf8');
const loader=fs.readFileSync('nova/live-support.js','utf8');

test('web Camera shows evidence with its source photo number',()=>{
  assert.match(provenance,/assessment\?\.evidence_by_photo/);
  assert.match(provenance,/Photo \$\{photo\}/);
  assert.match(provenance,/SOURCE/);
});

test('invalid provenance photo indexes are ignored',()=>{
  assert.match(provenance,/!Number\.isInteger\(photo\)\|\|photo<1\|\|photo>6/);
});

test('provenance renderer observes Camera result updates without changing Camera request logic',()=>{
  assert.match(provenance,/MutationObserver/);
  assert.match(provenance,/NovaCameraAssistant\?\.getLastAssessment/);
});

test('provenance display loads after Camera Assistant',()=>{
  const camera=loader.indexOf("camera-assistant.js?v=2");
  const display=loader.indexOf("camera-photo-provenance.js?v=1");
  assert.ok(camera>=0&&display>camera);
});
