import fs from 'node:fs';
import test from 'node:test';
import assert from 'node:assert/strict';

const evidence = JSON.parse(fs.readFileSync(new URL('../nova/catalogue-ipad8-japan-retail-identity-evidence.json', import.meta.url)));

test('iPad 8 Japan retail evidence preserves exact hardware identity', () => {
  assert.equal(evidence.candidate_row.id, 1188);
  assert.equal(evidence.candidate_row.retail_part_number, 'MYML2J/A');
  assert.equal(evidence.verified_identity.hardware_model_number, 'A2429');
  assert.equal(evidence.verified_identity.model_name, 'iPad (8th generation) Wi-Fi + Cellular');
  assert.equal(evidence.verified_identity.storage, '128GB');
  assert.equal(evidence.canonical_row.id, 1184);
  assert.equal(evidence.canonical_row.model_number, 'A2429');
});

test('iPad 8 Japan retail evidence remains read-only and region-safe', () => {
  assert.equal(evidence.execution_authorized, false);
  assert.equal(evidence.requires_explicit_human_authorization_for_production_write, true);
  assert.deepEqual(evidence.candidate_row.dependency_refs, {inventory:0, pricing:0, pricing_history:0});
  assert.ok(evidence.safety_notes.some(note => /No Supabase write/i.test(note)));
  assert.ok(evidence.safety_notes.some(note => /foreign retail part number/i.test(note)));
  assert.ok(evidence.safety_notes.some(note => /explicit human authorization/i.test(note)));
});