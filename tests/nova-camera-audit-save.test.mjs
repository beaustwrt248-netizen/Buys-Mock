import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const fn=fs.readFileSync('supabase/functions/nova-camera-audit-save/index.ts','utf8');
const client=fs.readFileSync('nova/camera-audit-save.js','utf8');
const loader=fs.readFileSync('nova/live-support.js','utf8');

test('Camera audit save is admin authenticated and writes only through the server function',()=>{
  assert.match(fn,/admin\.auth\.getUser\(token\)/);
  assert.match(fn,/profile\.role!=='admin'/);
  assert.match(fn,/admin_audit_log/);
  assert.match(fn,/nova_camera_assessment_saved/);
});

test('Camera audit deliberately excludes photos and full identifiers',()=>{
  assert.match(fn,/photos_stored:false/);
  assert.match(fn,/full_identifiers_stored:false/);
  assert.match(fn,/function mask\(/);
  assert.doesNotMatch(fn,/image_data_url/);
  assert.doesNotMatch(fn,/label_identifiers/);
});

test('saved final-offer readiness independently requires verified identity and complete passed functionality',()=>{
  assert.match(fn,/const identityVerified=market\.identity_verified===true/);
  assert.match(fn,/final_offer_ready:Boolean\(body\?\.final_offer_ready\)&&identityVerified&&func\.complete&&func\.failed\.length===0&&func\.pending\.length===0/);
});

test('client save is an explicit staff action and sends bounded structured state',()=>{
  assert.match(client,/Save assessment/);
  assert.match(client,/cameraAuditSaveBtn/);
  assert.match(client,/functionality_gate/);
  assert.match(client,/market_summary/);
  assert.match(client,/nova-camera-audit-save/);
});

test('audit module loads after Camera functionality gate',()=>{
  const functionality=loader.indexOf("camera-functionality-checklist.js?v=1");
  const audit=loader.indexOf("camera-audit-save.js?v=1");
  assert.ok(functionality>=0&&audit>functionality);
});
