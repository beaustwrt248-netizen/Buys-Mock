import fs from 'node:fs';
import test from 'node:test';
import assert from 'node:assert/strict';

const evidence = JSON.parse(fs.readFileSync(new URL('../nova/catalogue-xiaomi-model-number-evidence.json', import.meta.url)));

test('Xiaomi evidence records only the two verified model-number gaps', () => {
  assert.deepEqual(evidence.records.map(record => record.device_catalog_id), [927, 987]);
  assert.deepEqual(evidence.records.map(record => record.verified_model_number), ['25080RABDG', '2506PN68G']);
});

test('unresolved Xiaomi rows stay explicitly excluded', () => {
  assert.deepEqual(evidence.excluded_unresolved_rows, [891, 892, 866, 893, 894, 903]);
});

test('evidence remains read-only and human gated', () => {
  assert.equal(evidence.execution_authorized, false);
  assert.equal(evidence.requires_explicit_human_authorization_for_production_write, true);
  assert.ok(evidence.safety_notes.some(note => /No Supabase write/i.test(note)));
});
