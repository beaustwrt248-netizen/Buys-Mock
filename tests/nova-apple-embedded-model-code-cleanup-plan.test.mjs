import fs from 'node:fs';
import assert from 'node:assert/strict';

const plan = JSON.parse(fs.readFileSync('nova/catalogue-cleanup-apple-embedded-model-code-plan.json', 'utf8'));
assert.equal(plan.execution_authorized, false);
assert.equal(plan.requires_explicit_human_authorization, true);
assert.equal(plan.pre_execution_recheck_required, true);
assert.equal(plan.candidates.length, 3);

const expected = new Map([
  [1048, {canonical_id:537, model:'A2221'}],
  [1195, {canonical_id:133, model:'A3355'}],
  [1225, {canonical_id:1223, model:'A2133'}]
]);
for (const row of plan.candidates) {
  const match = expected.get(row.candidate_id);
  assert.ok(match, `unexpected candidate ${row.candidate_id}`);
  assert.equal(row.canonical_id, match.canonical_id);
  assert.equal(row.canonical_model_number, match.model);
  assert.deepEqual(row.candidate_dependency_refs, {inventory:0, pricing:0, pricing_history:0});
  assert.equal(row.proposed_action, 'deactivate_duplicate_candidate');
}
console.log('nova apple embedded model-code cleanup plan: PASS');
