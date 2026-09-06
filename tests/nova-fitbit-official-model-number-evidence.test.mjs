import fs from 'node:fs';
import test from 'node:test';
import assert from 'node:assert/strict';

const evidence = JSON.parse(fs.readFileSync(new URL('../nova/catalogue-fitbit-official-model-number-evidence.json', import.meta.url)));

test('Fitbit evidence keeps exact official model mappings', () => {
  assert.deepEqual(
    evidence.verified_records.map(record => [record.device_catalog_id, record.verified_model_number]),
    [[1007, 'FB507'], [1010, 'FB511'], [1009, 'FB512'], [1011, 'FB521'], [1012, 'FB523'], [1008, 'FB415']]
  );
  assert.deepEqual(evidence.unresolved_records[0].known_official_model_numbers, ['FB504', 'FB505']);
});

test('Fitbit evidence remains fail-closed and protects pricing-linked rows', () => {
  assert.equal(evidence.execution_authorized, false);
  assert.equal(evidence.requires_explicit_human_authorization_for_production_write, true);
  const versa4 = evidence.verified_records.find(record => record.device_catalog_id === 1012);
  assert.deepEqual(versa4.dependency_refs, {inventory:0, pricing:1, pricing_history:0});
  assert.ok(evidence.safety_notes.some(note => /Versa row 1006 remains unresolved/i.test(note)));
  assert.ok(evidence.safety_notes.some(note => /pricing reference on Versa 4/i.test(note)));
  assert.ok(evidence.safety_notes.some(note => /explicit human authorization/i.test(note)));
});