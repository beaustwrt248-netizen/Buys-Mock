import fs from 'node:fs';
import test from 'node:test';
import assert from 'node:assert/strict';

const evidence = JSON.parse(fs.readFileSync(new URL('../nova/catalogue-huawei-watch-gt6-model-evidence.json', import.meta.url)));

test('Huawei Watch GT6 evidence keeps exact size-specific model mappings', () => {
  const byId = new Map(evidence.records.map(record => [record.device_catalog_id, record]));
  assert.equal(byId.get(955).verified_model_number, 'KSU-B19');
  assert.equal(byId.get(956).verified_model_number, 'ATM-B19');
  assert.equal(byId.get(955).model_name, 'HUAWEI WATCH GT 6 41mm');
  assert.equal(byId.get(956).model_name, 'HUAWEI WATCH GT 6 46mm');
  assert.notEqual(byId.get(955).verified_model_number, byId.get(956).verified_model_number);
});

test('Huawei Watch GT6 evidence remains fail-closed and collision-safe', () => {
  assert.equal(evidence.execution_authorized, false);
  assert.equal(evidence.requires_explicit_human_authorization_for_production_write, true);
  assert.equal(evidence.collision_check['KSU-B19_active_rows'], 0);
  assert.equal(evidence.collision_check['ATM-B19_active_rows'], 0);
  assert.ok(evidence.records.every(record =>
    record.dependency_refs.inventory === 0 &&
    record.dependency_refs.pricing === 0 &&
    record.dependency_refs.pricing_history === 0
  ));
  assert.ok(evidence.manufacturer_sources.every(source => source.url.includes('consumer.huawei.com')));
  assert.ok(evidence.safety_notes.some(note => /must remain distinct/i.test(note)));
});