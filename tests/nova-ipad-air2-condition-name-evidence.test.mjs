import fs from 'node:fs';
import test from 'node:test';
import assert from 'node:assert/strict';

const evidence = JSON.parse(fs.readFileSync(new URL('../nova/catalogue-ipad-air2-condition-name-evidence.json', import.meta.url)));

test('iPad Air 2 condition-text evidence uses fresh live dependencies', () => {
  assert.equal(evidence.execution_authorized, false);
  assert.equal(evidence.requires_explicit_human_authorization_for_production_write, true);
  assert.equal(evidence.candidate_row.id, 1206);
  assert.equal(evidence.candidate_row.classification, 'reseller_condition_text_embedded_in_model_name');
  assert.deepEqual(evidence.candidate_row.dependency_refs, {inventory: 0, pricing: 0, pricing_history: 0});
  assert.deepEqual(evidence.related_rows.find(row => row.id === 1203)?.dependency_refs, {inventory: 0, pricing: 2, pricing_history: 0});
});

test('iPad Air 2 retail part mapping supports A1566 without mutating production', () => {
  assert.equal(evidence.candidate_row.proposed_canonical_identity.model_name, 'iPad Air 2 Wi-Fi');
  assert.equal(evidence.candidate_row.proposed_canonical_identity.model_number, 'A1566');
  assert.equal(evidence.candidate_row.proposed_canonical_identity.storage, '32GB');
  assert.deepEqual(evidence.first_party_identity.hardware_variants.map(item => item.model_number), ['A1566', 'A1567']);
  assert.ok(evidence.australian_retail_part_mapping.some(item => /MNV62X\/A.*32GB Wi-Fi/i.test(item.finding)));
  assert.ok(evidence.identity_reasoning.some(note => /A1566 as the Wi-Fi/i.test(note)));
  assert.ok(evidence.safety_notes.some(note => /No Supabase write/i.test(note)));
  assert.ok(evidence.safety_notes.some(note => /explicit human authorization/i.test(note)));
});
