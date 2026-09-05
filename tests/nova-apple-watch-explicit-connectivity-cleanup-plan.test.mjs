import fs from 'node:fs';
import assert from 'node:assert/strict';

const plan = JSON.parse(fs.readFileSync('nova/catalogue-cleanup-apple-watch-explicit-connectivity-plan.json','utf8'));
assert.equal(plan.execution_authorized,false);
assert.equal(plan.requires_explicit_human_authorization,true);
assert.equal(plan.pre_execution_recheck_required,true);
assert.equal(plan.candidates.length,2);
assert.equal(plan.pricing_blockers.length,6);
assert.deepEqual(plan.excluded_ambiguous_rows,[1134,1135,1137,1138,1139,1143,1146,1148]);

const candidates = new Map(plan.candidates.map(row=>[row.candidate_id,row]));
assert.equal(candidates.get(1141)?.canonical_id,263);
assert.equal(candidates.get(1141)?.canonical_model_number,'A3337');
assert.equal(candidates.get(1145)?.canonical_id,303);
assert.equal(candidates.get(1145)?.canonical_model_number,'A2684');
for (const row of plan.candidates) {
  assert.deepEqual(row.candidate_dependency_refs,{inventory:0,pricing:0,pricing_history:0});
  assert.equal(row.proposed_action,'deactivate_duplicate_candidate');
}
for (const row of plan.pricing_blockers) assert.equal(row.candidate_pricing_refs,1);

console.log('nova apple watch explicit-connectivity cleanup plan: PASS');
