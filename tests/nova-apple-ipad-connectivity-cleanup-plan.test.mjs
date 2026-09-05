import fs from 'node:fs';
import assert from 'node:assert/strict';

const plan = JSON.parse(fs.readFileSync('nova/catalogue-cleanup-apple-ipad-connectivity-plan.json', 'utf8'));
assert.equal(plan.execution_authorized, false);
assert.equal(plan.requires_explicit_human_authorization, true);
assert.equal(plan.pre_execution_recheck_required, true);
assert.deepEqual(plan.excluded_ambiguous_rows, [1158,1159]);
assert.equal(plan.candidates.length, 4);

const expected = new Map([
  [1160, {canonical_id:1157, model:'A2757', storage:'256GB'}],
  [1161, {canonical_id:134, model:'A2696', storage:'64GB'}],
  [1172, {canonical_id:139, model:'A1822', storage:'128GB'}],
  [1173, {canonical_id:1167, model:'A1823', storage:'32GB'}]
]);
for (const row of plan.candidates) {
  const match = expected.get(row.candidate_id);
  assert.ok(match, `unexpected candidate ${row.candidate_id}`);
  assert.equal(row.canonical_id, match.canonical_id);
  assert.equal(row.canonical_model_number, match.model);
  assert.deepEqual(row.candidate_storage_options, [match.storage]);
  assert.deepEqual(row.candidate_dependency_refs, {inventory:0, pricing:0, pricing_history:0});
  assert.equal(row.proposed_action, 'deactivate_duplicate_candidate');
}
console.log('nova apple ipad connectivity cleanup plan: PASS');
