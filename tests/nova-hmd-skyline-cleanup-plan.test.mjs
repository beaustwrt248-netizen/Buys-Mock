import assert from 'node:assert/strict';
import fs from 'node:fs';

const plan = JSON.parse(fs.readFileSync('nova/catalogue-cleanup-hmd-skyline-plan.json', 'utf8'));

assert.equal(plan.change_type, 'production_data_cleanup_candidate');
assert.equal(plan.execution_authorized, false);
assert.equal(plan.requires_explicit_human_authorization, true);
assert.equal(plan.candidate_count, 1);
assert.equal(plan.candidates.length, 1);

const row = plan.candidates[0];
assert.equal(row.candidate_id, 1078);
assert.equal(row.canonical_id, 800);
assert.equal(row.candidate_brand, 'HMD');
assert.equal(row.canonical_brand, 'HMD');
assert.equal(row.candidate_model_number, 'TA-1600');
assert.match(row.canonical_model_number, /TA-1600/);
assert.deepEqual(row.candidate_storage, ['256GB']);
assert.deepEqual(row.canonical_storage, ['256GB']);
assert.equal(row.inventory_refs, 0);
assert.equal(row.pricing_refs, 0);
assert.equal(row.pricing_history_refs, 0);
assert.equal(row.status, 'ready_for_explicit_approval_after_fresh_recheck');
assert.match(plan.safety_note, /explicit human authorization/i);
assert.match(plan.safety_note, /fresh re-query/i);
