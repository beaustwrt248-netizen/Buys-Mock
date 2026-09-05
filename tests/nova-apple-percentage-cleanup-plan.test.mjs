import assert from 'node:assert/strict';
import fs from 'node:fs';

const plan = JSON.parse(fs.readFileSync('nova/catalogue-cleanup-apple-percentage-text-plan.json', 'utf8'));

assert.equal(plan.change_type, 'production_data_cleanup_candidate');
assert.equal(plan.execution_authorized, false);
assert.equal(plan.requires_explicit_human_authorization, true);
assert.equal(plan.candidate_count, 9);
assert.equal(plan.safe_unreferenced_candidate_count, 8);
assert.equal(plan.pricing_blocked_candidate_count, 1);
assert.equal(plan.candidates.length, 9);

const ids = plan.candidates.map(row => row.candidate_id);
assert.deepEqual(ids, [1049,1050,1051,1054,1058,1059,1060,1062,1063]);
assert.equal(new Set(ids).size, ids.length);

for (const row of plan.candidates.slice(0, 8)) {
  assert.equal(row.proposed_action, 'deactivate_duplicate_candidate');
  assert.equal(row.inventory_refs, 0);
  assert.equal(row.pricing_refs, 0);
  assert.equal(row.pricing_history_refs, 0);
  assert.equal(row.status, 'ready_for_explicit_approval_after_fresh_recheck');
  assert.match(row.candidate_model_name, /(?:\d{1,3}%|batt\s*health)/i);
  assert.ok(Number.isInteger(row.canonical_id) && row.canonical_id > 0);
  assert.ok(String(row.canonical_model_number || '').length > 0);
}

const blocked = plan.candidates.find(row => row.candidate_id === 1063);
assert.equal(blocked?.proposed_action, 'none');
assert.equal(blocked?.pricing_refs, 1);
assert.equal(blocked?.status, 'blocked_protected_pricing');
assert.match(plan.safety_note, /explicit human authorization/i);
assert.match(plan.safety_note, /Re-query/i);
assert.match(plan.safety_note, /1063/);
