import fs from 'node:fs';
import test from 'node:test';
import assert from 'node:assert/strict';

const evidence = JSON.parse(fs.readFileSync(new URL('../nova/catalogue-apple-watch-se2-retail-duplicate-evidence-v2.json', import.meta.url)));

test('Apple Watch SE2 duplicate evidence preserves exact AU hardware identity', () => {
  assert.equal(evidence.canonical_record.device_catalog_id, 314);
  assert.equal(evidence.canonical_record.model_number, 'A2724');
  assert.equal(evidence.candidate_record.device_catalog_id, 1143);
  assert.equal(evidence.candidate_record.market_region, 'AU');
  assert.deepEqual(evidence.canonical_record.storage_options, ['32GB']);
  assert.deepEqual(evidence.candidate_record.storage_options, ['32GB']);
  assert.equal(evidence.assessment, 'candidate_duplicate_of_canonical');
});

test('Apple Watch SE2 duplicate evidence remains fail-closed around protected pricing', () => {
  assert.equal(evidence.execution_authorized, false);
  assert.equal(evidence.requires_explicit_human_authorization_for_production_write, true);
  assert.equal(evidence.canonical_record.dependency_refs.pricing, 1);
  assert.equal(evidence.candidate_record.dependency_refs.pricing, 1);
  assert.ok(evidence.manufacturer_evidence.url.startsWith('https://support.apple.com/en-au/'));
  assert.ok(evidence.safety_notes.some(note => /No pricing reference is moved/i.test(note)));
  assert.ok(evidence.safety_notes.some(note => /explicit human authorization/i.test(note)));
});