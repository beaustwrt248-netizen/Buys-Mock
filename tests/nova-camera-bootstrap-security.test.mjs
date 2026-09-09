import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const read = (path) => fs.readFileSync(path, 'utf8');

const loader = read('nova/live-support.js');
const auditClient = read('nova/camera-audit-save.js');
const auditFunction = read('supabase/functions/nova-camera-audit-save/index.ts');

const modules = [
  'camera-valuation-safety.js?v=1',
  'camera-image-preprocessor.js?v=1',
  'camera-assistant.js?v=2',
  'camera-live-preview.js?v=1',
  'camera-approved-base-price.js?v=1',
  'camera-photo-provenance.js?v=1',
  'camera-functionality-checklist.js?v=1',
  'camera-audit-save.js?v=1',
];

test('Camera modules load exactly once and in safety-first order', () => {
  let previous = -1;
  for (const module of modules) {
    const first = loader.indexOf(module);
    assert.notEqual(first, -1, `${module} must be loaded`);
    assert.equal(loader.indexOf(module, first + 1), -1, `${module} must load only once`);
    assert.ok(first > previous, `${module} must load after the preceding Camera module`);
    previous = first;
  }
});

test('Camera audit save remains an explicit authenticated structured-only action', () => {
  assert.match(auditClient, /getAccessToken/);
  assert.match(auditClient, /functions\/v1\/nova-camera-audit-save/);
  assert.match(auditClient, /method:'POST'/);
  assert.match(auditClient, /Save assessment/);
  assert.match(auditClient, /structured evidence only; photos were not stored/);
  assert.doesNotMatch(auditClient, /image_data_url|image_data_urls|data:image\//);
});

test('Camera audit Edge Function preserves admin, privacy and no-write boundaries', () => {
  assert.match(auditFunction, /profile\.role!=='admin'/);
  assert.match(auditFunction, /profile\?\.is_enabled/);
  assert.match(auditFunction, /photos_stored:false/);
  assert.match(auditFunction, /full_identifiers_stored:false/);
  assert.match(auditFunction, /action:'nova_camera_assessment_saved'/);
  assert.match(auditFunction, /admin_audit_log/);
  assert.doesNotMatch(auditFunction, /from\('device_catalog'\)\.insert|from\('device_catalog'\)\.update/);
  assert.doesNotMatch(auditFunction, /from\('device_buy_prices'\)\.insert|from\('device_buy_prices'\)\.update/);
  assert.doesNotMatch(auditFunction, /image_data_url|image_data_urls|data:image\//);
});
