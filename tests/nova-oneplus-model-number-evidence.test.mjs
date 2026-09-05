import fs from 'node:fs';
import test from 'node:test';
import assert from 'node:assert/strict';

const evidence = JSON.parse(fs.readFileSync(new URL('../nova/catalogue-oneplus-model-number-evidence.json', import.meta.url)));

test('OnePlus evidence records the five verified identifiers', () => {
  assert.deepEqual(evidence.records.map(record => record.device_catalog_id), [901, 865, 976, 900, 928]);
  assert.deepEqual(evidence.records.map(record => record.verified_model_number), ['CPH2723', 'CPH2747', 'CPH2769', 'CPH2709', 'CPH2719']);
});

test('OnePlus evidence preserves the global Nord 5 variant decision', () => {
  const nord5 = evidence.records.find(record => record.device_catalog_id === 900);
  assert.equal(nord5.verified_model_number, 'CPH2709');
  assert.match(nord5.evidence.join(' '), /EU\/GLO.*CPH2709/i);
});

test('OnePlus evidence remains read-only and human gated', () => {
  assert.equal(evidence.execution_authorized, false);
  assert.equal(evidence.requires_explicit_human_authorization_for_production_write, true);
  assert.ok(evidence.safety_notes.some(note => /No Supabase write/i.test(note)));
  assert.ok(evidence.safety_notes.some(note => /regional-variant mismatch/i.test(note)));
});
