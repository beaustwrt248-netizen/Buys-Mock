import fs from 'node:fs';
import test from 'node:test';
import assert from 'node:assert/strict';

const evidence = JSON.parse(fs.readFileSync(new URL('../nova/catalogue-xiaomi-watch-s4-model-evidence.json', import.meta.url)));

test('Xiaomi Watch S4 evidence keeps exact first-party model mapping', () => {
  assert.equal(evidence.record.device_catalog_id, 958);
  assert.equal(evidence.record.model_name, 'Xiaomi Watch S4');
  assert.equal(evidence.record.verified_model_number, 'M2425W1');
  assert.equal(evidence.record.market_region, 'Australia');
  assert.equal(evidence.assessment, 'manufacturer_model_number_verified');
  assert.ok(evidence.manufacturer_sources.every(source => source.url.includes('mi.com')));
});

test('Xiaomi Watch S4 evidence remains fail-closed and collision-safe', () => {
  assert.equal(evidence.execution_authorized, false);
  assert.equal(evidence.requires_explicit_human_authorization_for_production_write, true);
  assert.deepEqual(evidence.record.dependency_refs, {inventory: 0, pricing: 0, pricing_history: 0});
  assert.equal(evidence.collision_check.existing_active_catalogue_rows, 0);
  assert.ok(evidence.safety_notes.some(note => /explicit human authorization/i.test(note)));
  assert.ok(evidence.safety_notes.some(note => /distinct Australian hardware variant/i.test(note)));
});