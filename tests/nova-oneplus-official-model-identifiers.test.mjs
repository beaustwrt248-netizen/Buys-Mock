import fs from 'node:fs';
import assert from 'node:assert/strict';

const evidence = JSON.parse(fs.readFileSync('nova/catalogue-oneplus-official-model-identifiers.json','utf8'));
assert.equal(evidence.execution_authorized, false);
assert.equal(evidence.requires_explicit_human_authorization_for_production_write, true);
assert.match(evidence.manufacturer_authority.url, /^https:\/\/www\.oneplus\.com\/uk\/pstisoc/);
const expected = new Map([[865,'CPH2747'],[900,'CPH2709'],[928,'CPH2719'],[957,'OPWWE251']]);
for (const row of evidence.rows) {
  assert.equal(row.current_model_number, null);
  assert.equal(row.verified_official_model_number, expected.get(row.id));
  assert.deepEqual(row.dependency_refs,{inventory:0,pricing:0,pricing_history:0});
  assert.match(row.regional_caution,/reconfirm/i);
}
assert.equal(evidence.rows.length,4);
assert.ok(evidence.safety_notes.some(note=>/No Supabase write/i.test(note)));
assert.ok(evidence.safety_notes.some(note=>/does not authorize/i.test(note)));
assert.ok(evidence.safety_notes.some(note=>/regional-identity recheck/i.test(note)));
console.log('nova OnePlus official model identifiers: PASS');
