import fs from 'node:fs';
import test from 'node:test';
import assert from 'node:assert/strict';

const evidence = JSON.parse(fs.readFileSync(new URL('../nova/catalogue-realme-model-number-evidence.json', import.meta.url)));

test('Realme evidence records the three verified identifiers', () => {
  assert.deepEqual(evidence.records.map(record => record.device_catalog_id), [872, 873, 916]);
  assert.deepEqual(evidence.records.map(record => record.verified_model_number), ['RMX5120', 'RMX5131', 'RMX5210']);
});

test('GT 8 Pro preserves regional variant distinction', () => {
  const gt8 = evidence.records.find(record => record.device_catalog_id === 916);
  assert.equal(gt8.verified_model_number, 'RMX5210');
  assert.match(gt8.evidence.join(' '), /Italy|EU|global/i);
  assert.match(evidence.regional_variant_notes.RMX5200, /China variant/i);
});

test('Realme evidence remains read-only and human gated', () => {
  assert.equal(evidence.execution_authorized, false);
  assert.equal(evidence.requires_explicit_human_authorization_for_production_write, true);
  assert.ok(evidence.safety_notes.some(note => /No Supabase write/i.test(note)));
});
