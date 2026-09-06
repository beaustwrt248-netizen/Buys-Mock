import fs from 'node:fs';
import assert from 'node:assert/strict';

const evidence = JSON.parse(fs.readFileSync('nova/huawei-wearable-model-number-evidence.json', 'utf8'));
assert.equal(evidence.execution_authorized, false);
assert.equal(evidence.requires_explicit_human_authorization, true);
assert.equal(evidence.pre_execution_recheck_required, true);
assert.equal(evidence.rows.length, 2);

const expected = new Map([
  [955, 'KSU-B19'],
  [956, 'ATM-B19']
]);

for (const row of evidence.rows) {
  assert.equal(row.verified_model_number, expected.get(row.id), `unexpected model number for ${row.id}`);
  assert.deepEqual(row.dependency_refs, {inventory:0, pricing:0, pricing_history:0});
  assert.match(row.evidence_url, /^https:\/\/consumer\.huawei\.com\//);
}

assert.equal(evidence.rows.some(row => row.id === 954), false);
assert.equal(evidence.explicitly_unresolved.length, 1);
const fit4 = evidence.explicitly_unresolved[0];
assert.equal(fit4.id, 954);
assert.equal(fit4.market_region, 'Australia');
assert.deepEqual(fit4.known_first_party_variant_codes.sort(), ['SYA-B09','SYA-B19']);
assert.match(fit4.reason, /Do not assign a universal code/i);
assert.deepEqual(fit4.dependency_refs, {inventory:0, pricing:0, pricing_history:0});

assert.ok(evidence.safety_notes.some(note => /No Supabase write/i.test(note)));
assert.ok(evidence.safety_notes.some(note => /deliberately unresolved/i.test(note)));
assert.ok(evidence.safety_notes.some(note => /Re-query/i.test(note)));
console.log('nova huawei wearable model-number evidence: PASS');
