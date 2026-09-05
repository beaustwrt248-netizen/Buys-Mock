import assert from 'node:assert/strict';
import fs from 'node:fs';

const plan = JSON.parse(fs.readFileSync('nova/catalogue-cleanup-apple-trailing-percent-plan.json', 'utf8'));

assert.equal(plan.change_type, 'production_data_cleanup_candidate');
assert.equal(plan.execution_authorized, false, 'cleanup plan must never imply execution is authorized');
assert.equal(plan.requires_explicit_human_authorization, true, 'production cleanup must remain human controlled');
assert.equal(plan.candidate_count, 9, 'current audited candidate set must contain exactly nine rows');
assert.equal(plan.candidates.length, plan.candidate_count, 'candidate count must reconcile');

const candidateIds = new Set();
const canonicalIds = new Set();
for (const row of plan.candidates) {
  assert.ok(Number.isInteger(row.candidate_id) && row.candidate_id > 0);
  assert.ok(Number.isInteger(row.canonical_id) && row.canonical_id > 0);
  assert.notEqual(row.candidate_id, row.canonical_id, 'candidate and canonical row must be different records');
  assert.equal(row.proposed_action, 'deactivate_duplicate_candidate');
  assert.match(String(row.candidate_model_name), /(?:\d{1,3}\s*%|Batt Health)/i, 'candidate must retain audited listing-specific signal');
  assert.ok(String(row.canonical_model_name || '').length > 0);
  assert.ok(String(row.canonical_model_number || '').length > 0, 'canonical target must have a verified model number');
  assert.ok(!candidateIds.has(row.candidate_id), 'candidate IDs must be unique');
  candidateIds.add(row.candidate_id);
  canonicalIds.add(row.canonical_id);
}

assert.equal(candidateIds.size, 9);
assert.equal(canonicalIds.size, 9, 'each contaminated row must map one-to-one to a canonical target');
assert.match(plan.safety_note, /Do not execute/i);
assert.match(plan.safety_note, /explicit human authorization/i);
assert.match(plan.safety_note, /Re-query/i, 'plan must require drift detection immediately before any future mutation');

console.log('Nova Apple catalogue cleanup plan safety contract: PASS');
