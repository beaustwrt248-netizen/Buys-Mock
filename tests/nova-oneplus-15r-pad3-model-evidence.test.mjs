import fs from 'node:fs';
import test from 'node:test';
import assert from 'node:assert/strict';

const evidence = JSON.parse(fs.readFileSync(new URL('../nova/catalogue-oneplus-15r-pad3-model-evidence.json', import.meta.url)));

test('OnePlus evidence keeps exact manufacturer model mappings', () => {
  assert.deepEqual(
    evidence.records.map(record => [record.device_catalog_id, record.verified_model_number]),
    [[976, 'CPH2769'], [950, 'OPD2415']]
  );
  assert.ok(evidence.records.every(record => record.evidence_urls.some(url => url.includes('oneplus.com'))));
});

test('OnePlus evidence remains fail-closed and dependency-safe', () => {
  assert.equal(evidence.execution_authorized, false);
  assert.equal(evidence.requires_explicit_human_authorization_for_production_write, true);
  assert.ok(evidence.records.every(record =>
    record.dependency_refs.inventory === 0 &&
    record.dependency_refs.pricing === 0 &&
    record.dependency_refs.pricing_history === 0
  ));
  assert.ok(evidence.safety_notes.some(note => /explicit human authorization/i.test(note)));
  assert.ok(evidence.safety_notes.some(note => /regional ambiguity/i.test(note)));
});