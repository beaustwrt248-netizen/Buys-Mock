import fs from 'node:fs';
import test from 'node:test';
import assert from 'node:assert/strict';

const evidence = JSON.parse(fs.readFileSync(new URL('../nova/catalogue-ipad-air2-condition-name-evidence.json', import.meta.url)));

test('iPad Air 2 condition-text evidence preserves exact uncertainty', () => {
  assert.equal(evidence.execution_authorized, false);
  assert.equal(evidence.requires_explicit_human_authorization_for_production_write, true);
  assert.equal(evidence.candidate_row.id, 1206);
  assert.equal(evidence.candidate_row.current_model_number, null);
  assert.equal(evidence.candidate_row.classification, 'reseller_condition_text_embedded_in_model_name');
  assert.deepEqual(evidence.candidate_row.dependency_refs, {inventory: 0, pricing: 1, pricing_history: 0});
});

test('iPad Air 2 evidence does not collapse Apple hardware variants', () => {
  assert.deepEqual(
    evidence.first_party_identity.hardware_variants.map(item => item.model_number),
    ['A1566', 'A1567']
  );
  assert.ok(evidence.safety_notes.some(note => /Do not infer A1566 versus A1567/i.test(note)));
  assert.ok(evidence.safety_notes.some(note => /pricing reference on row 1206 is protected/i.test(note)));
  assert.ok(evidence.safety_notes.some(note => /No Supabase write/i.test(note)));
});