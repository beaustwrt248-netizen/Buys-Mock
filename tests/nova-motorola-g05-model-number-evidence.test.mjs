import fs from 'node:fs';
import assert from 'node:assert/strict';

const evidence = JSON.parse(fs.readFileSync('nova/catalogue-motorola-g05-model-number-evidence.json', 'utf8'));

assert.equal(evidence.execution_authorized, false);
assert.equal(evidence.requires_explicit_human_authorization_for_production_write, true);
assert.equal(evidence.catalogue_row.id, 1083);
assert.equal(evidence.catalogue_row.current_model_number, null);
assert.deepEqual(evidence.catalogue_row.dependency_refs, {inventory:0, pricing:0, pricing_history:0});
assert.deepEqual(evidence.verified_first_party_model_family, ['XT2523-3', 'XT2523-11']);
assert.equal(evidence.unsupported_catalogue_alias_fragment, 'Xt259-2');
assert.equal(evidence.australian_exact_model_number_status, 'unresolved');
assert.ok(evidence.conclusion.some(note => /Do not guess an Australian exact model number/i.test(note)));
assert.ok(evidence.conclusion.some(note => /Do not infer 256GB/i.test(note)));
assert.ok(evidence.safety_notes.some(note => /No Supabase write/i.test(note)));
console.log('nova Motorola moto g05 model-number evidence: PASS');
