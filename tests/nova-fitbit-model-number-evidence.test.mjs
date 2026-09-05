import fs from 'node:fs';
import test from 'node:test';
import assert from 'node:assert/strict';

const evidence = JSON.parse(fs.readFileSync(new URL('../nova/catalogue-fitbit-model-number-evidence.json', import.meta.url)));

test('Fitbit evidence records all seven verified identifiers', () => {
  assert.deepEqual(evidence.records.map(record => record.device_catalog_id), [1009,1011,1006,1007,1010,1012,1008]);
  assert.deepEqual(evidence.records.map(record => record.verified_model_number), ['FB512','FB521','FB504','FB507','FB511','FB523','FB415']);
});

test('Fitbit evidence stays read-only and human gated', () => {
  assert.equal(evidence.execution_authorized, false);
  assert.equal(evidence.requires_explicit_human_authorization_for_production_write, true);
  assert.ok(evidence.safety_notes.some(note => /No Supabase write/i.test(note)));
  assert.ok(evidence.safety_notes.some(note => /Abort on row drift/i.test(note)));
});
