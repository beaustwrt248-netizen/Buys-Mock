import fs from 'node:fs';
import assert from 'node:assert/strict';

const plan=JSON.parse(fs.readFileSync('nova/catalogue-cleanup-apple-ipad-exact-connectivity-plan.json','utf8'));
assert.equal(plan.execution_authorized,false);
assert.equal(plan.requires_explicit_human_authorization,true);
assert.equal(plan.pre_execution_recheck_required,true);
assert.equal(plan.candidates.length,5);
const expected=new Map([
  [1183,[137,'A2197','128GB']],
  [1194,[133,'A3355','256GB']],
  [1237,[1239,'A3358','256GB']],
  [1238,[1251,'A2759','128GB']],
  [1265,[1243,'A2230','128GB']]
]);
for(const row of plan.candidates){
  const match=expected.get(row.candidate_id); assert.ok(match);
  assert.equal(row.canonical_id,match[0]);
  assert.equal(row.canonical_model_number,match[1]);
  assert.deepEqual(row.candidate_storage_options,[match[2]]);
  assert.deepEqual(row.candidate_dependency_refs,{inventory:0,pricing:0,pricing_history:0});
  assert.equal(row.proposed_action,'deactivate_duplicate_candidate');
}
assert.ok(plan.excluded_ambiguous_rows.includes(1207));
assert.ok(plan.excluded_ambiguous_rows.includes(1232));
console.log('nova apple ipad exact-connectivity cleanup plan: PASS');
