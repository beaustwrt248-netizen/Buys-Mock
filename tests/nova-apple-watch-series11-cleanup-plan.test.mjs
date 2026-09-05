import fs from 'node:fs';
import assert from 'node:assert/strict';

const plan = JSON.parse(fs.readFileSync('nova/catalogue-cleanup-apple-watch-series11-plan.json', 'utf8'));

assert.equal(plan.execution_authorized, false);
assert.equal(plan.requires_explicit_human_authorization, true);
assert.equal(plan.pre_execution_recheck_required, true);
assert.equal(plan.candidates.length, 1);

const [row] = plan.candidates;
assert.equal(row.candidate_id, 1136);
assert.equal(row.canonical_id, 274);
assert.equal(row.canonical_model_number, 'A3333');
assert.deepEqual(row.canonical_storage_options, ['64GB']);
assert.deepEqual(row.candidate_dependency_refs, {inventory:0, pricing:0, pricing_history:0});
assert.equal(row.proposed_action, 'deactivate_duplicate_candidate');

console.log('nova apple watch series 11 cleanup plan: PASS');
