import fs from 'node:fs';
import assert from 'node:assert/strict';

const evidence = JSON.parse(fs.readFileSync('nova/catalogue-apple-watch-se2-retail-duplicate-evidence.json', 'utf8'));

assert.equal(evidence.execution_authorized, false);
assert.equal(evidence.requires_explicit_human_authorization_for_production_write, true);
assert.equal(evidence.canonical_row.id, 314);
assert.equal(evidence.canonical_row.model_number, 'A2724');
assert.equal(evidence.canonical_row.release_year, 2022);
assert.deepEqual(evidence.canonical_row.dependency_refs, {inventory:0, pricing:1, pricing_history:0});
assert.equal(evidence.retail_configuration_row.id, 1143);
assert.equal(evidence.retail_configuration_row.retail_part_number, 'MNQ23ZP/A');
assert.equal(evidence.retail_configuration_row.verified_asia_pacific_model_number, 'A2724');
assert.equal(evidence.retail_configuration_row.verified_release_year, 2022);
assert.equal(evidence.retail_configuration_row.verified_storage, '32GB');
assert.deepEqual(evidence.retail_configuration_row.dependency_refs, {inventory:0, pricing:1, pricing_history:0});
assert.ok(evidence.safety_notes.some(note => /No Supabase write/i.test(note)));
assert.ok(evidence.safety_notes.some(note => /active pricing references/i.test(note)));
assert.ok(evidence.safety_notes.some(note => /A-series hardware model number/i.test(note)));
console.log('nova Apple Watch SE 2 retail duplicate evidence: PASS');
