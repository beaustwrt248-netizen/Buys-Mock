import fs from 'node:fs';
import assert from 'node:assert/strict';

const evidence = JSON.parse(fs.readFileSync('nova/catalogue-oneplus-remaining-model-identifiers.json','utf8'));
assert.equal(evidence.execution_authorized, false);
assert.equal(evidence.requires_explicit_human_authorization_for_production_write, true);
const expected = new Map([[901,'CPH2723'],[976,'CPH2769'],[950,'OPD2415']]);
for (const row of evidence.rows) {
  assert.equal(row.current_model_number, null);
  assert.equal(row.verified_official_model_number, expected.get(row.id));
  assert.deepEqual(row.dependency_refs,{inventory:0,pricing:0,pricing_history:0});
  assert.match(row.regional_caution,/reconfirm|retain country\/region provenance/i);
}
assert.equal(evidence.rows.length,3);
assert.ok(evidence.safety_notes.some(note=>/No Supabase write/i.test(note)));
assert.ok(evidence.safety_notes.some(note=>/does not itself authorize/i.test(note)));
assert.ok(evidence.safety_notes.some(note=>/regional-identity recheck/i.test(note)));
console.log('nova remaining OnePlus model identifiers: PASS');
