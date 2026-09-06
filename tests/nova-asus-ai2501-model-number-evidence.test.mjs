import fs from 'node:fs';
import test from 'node:test';
import assert from 'node:assert/strict';

const evidence = JSON.parse(fs.readFileSync(new URL('../nova/catalogue-asus-ai2501-model-number-evidence.json', import.meta.url)));

test('ASUS evidence distinguishes model number from model name', () => {
  const byId = new Map(evidence.records.map(record => [record.device_catalog_id, record]));
  assert.equal(byId.get(854).verified_model_number, 'AI2501');
  assert.equal(byId.get(860).verified_model_number, 'AI2501');
  assert.equal(byId.get(1413).verified_model_number, 'AI2501H');
  assert.equal(byId.get(1413).assessment, 'model_number_correction_candidate');
  assert.equal(evidence.collision_check.existing_active_catalogue_rows, 0);
});

test('ASUS evidence remains fail-closed and avoids false deduplication', () => {
  assert.equal(evidence.execution_authorized, false);
  assert.equal(evidence.requires_explicit_human_authorization_for_production_write, true);
  assert.ok(evidence.records.every(record =>
    record.dependency_refs.inventory === 0 &&
    record.dependency_refs.pricing === 0 &&
    record.dependency_refs.pricing_history === 0
  ));
  assert.ok(evidence.manufacturer_source.url.includes('asus.com/au/support/'));
  assert.ok(evidence.safety_notes.some(note => /must not be deduplicated solely/i.test(note)));
  assert.ok(evidence.safety_notes.some(note => /explicit human authorization/i.test(note)));
});