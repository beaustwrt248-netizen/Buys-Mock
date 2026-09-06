import fs from 'node:fs';
import assert from 'node:assert/strict';

const evidence = JSON.parse(fs.readFileSync('nova/catalogue-ipad8-japan-retail-duplicate-evidence.json','utf8'));
assert.equal(evidence.execution_authorized, false);
assert.equal(evidence.requires_explicit_human_authorization_for_production_write, true);
assert.equal(evidence.canonical_row.id, 1184);
assert.equal(evidence.canonical_row.model_number, 'A2429');
assert.deepEqual(evidence.canonical_row.dependency_refs, {inventory:0,pricing:0,pricing_history:0});
assert.equal(evidence.retail_configuration_row.id, 1188);
assert.equal(evidence.retail_configuration_row.retail_part_number, 'MYML2J/A');
assert.equal(evidence.retail_configuration_row.verified_model_number, 'A2429');
assert.equal(evidence.retail_configuration_row.verified_storage, '128GB');
assert.equal(evidence.retail_configuration_row.verified_release_year, 2020);
assert.deepEqual(evidence.retail_configuration_row.dependency_refs, {inventory:0,pricing:0,pricing_history:0});
assert.ok(evidence.safety_notes.some(note => /No Supabase write/i.test(note)));
assert.ok(evidence.safety_notes.some(note => /does not authorize production cleanup/i.test(note)));
console.log('nova iPad 8 Japan retail duplicate evidence: PASS');
