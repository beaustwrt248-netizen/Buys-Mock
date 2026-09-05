import fs from 'node:fs';
import test from 'node:test';
import assert from 'node:assert/strict';

const evidence = JSON.parse(fs.readFileSync(new URL('../nova/catalogue-samsung-model-number-evidence.json', import.meta.url)));

test('Samsung evidence records the three verified Fold8/Flip8 identifiers', () => {
  assert.deepEqual(evidence.records.map(record => record.device_catalog_id), [967, 965, 966]);
  assert.deepEqual(evidence.records.map(record => record.verified_model_number), ['SM-F776B', 'SM-F971B', 'SM-F976B']);
});

test('Samsung evidence remains read-only and human gated', () => {
  assert.equal(evidence.execution_authorized, false);
  assert.equal(evidence.requires_explicit_human_authorization_for_production_write, true);
  assert.ok(evidence.safety_notes.some(note => /No Supabase write/i.test(note)));
  assert.ok(evidence.safety_notes.some(note => /Abort on row drift/i.test(note)));
});
