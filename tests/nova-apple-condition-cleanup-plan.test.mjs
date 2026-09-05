import assert from 'node:assert/strict';
import fs from 'node:fs';

const plan = JSON.parse(fs.readFileSync('nova/catalogue-cleanup-apple-condition-text-plan.json', 'utf8'));

assert.equal(plan.change_type, 'production_data_cleanup_candidate');
assert.equal(plan.execution_authorized, false, 'cleanup plan must never imply execution is authorized');
assert.equal(plan.requires_explicit_human_authorization, true, 'production cleanup must remain human controlled');
assert.equal(plan.candidate_count, 3, 'current audited condition-text candidate set must contain exactly three rows');
assert.equal(plan.candidates.length, plan.candidate_count, 'candidate count must reconcile');

const candidateIds = new Set();
const canonicalIds = new Set();
for (const row of plan.candidates) {
  assert.ok(Number.isInteger(row.candidate_id) && row.candidate_id > 0);
  assert.ok(Number.isInteger(row.canonical_id) && row.canonical_id > 0);
  assert.notEqual(row.candidate_id, row.canonical_id, 'candidate and canonical row must be different records');
  assert.equal(row.proposed_action, 'deactivate_duplicate_candidate');
  assert.match(String(row.candidate_model_name), /(cracked|doesnt turn on|sold as is|no warranty|cant update)/i, 'candidate must retain audited listing-specific condition/failure signal');
  assert.ok(String(row.canonical_model_name || '').length > 0);
  assert.ok(Array.isArray(row.candidate_storage));
  assert.ok(Array.isArray(row.canonical_storage));
  for (const storage of row.candidate_storage) {
    assert.ok(row.canonical_storage.includes(storage), `canonical target must cover candidate storage ${storage}`);
  }
  assert.ok(!candidateIds.has(row.candidate_id), 'candidate IDs must be unique');
  assert.ok(!canonicalIds.has(row.canonical_id), 'canonical target IDs must be unique');
  candidateIds.add(row.candidate_id);
  canonicalIds.add(row.canonical_id);
}

const ipad = plan.candidates.find(row => row.candidate_id === 1206);
assert.equal(ipad?.canonical_id, 1203);
assert.equal(ipad?.canonical_model_number, 'A1566', 'iPad Air 2 Wi-Fi target must retain its verified model number');
assert.equal(plan.candidates.find(row => row.candidate_id === 1070)?.canonical_model_number, null, 'iPhone 8 Plus model number remains unresolved and must not be guessed');
assert.equal(plan.candidates.find(row => row.candidate_id === 1075)?.canonical_model_number, null, 'iPhone XS Max model number remains unresolved and must not be guessed');

assert.match(plan.safety_note, /Do not execute/i);
assert.match(plan.safety_note, /explicit human authorization/i);
assert.match(plan.safety_note, /Re-query/i);
assert.match(plan.safety_note, /must not be guessed/i);

console.log('Nova Apple condition-text cleanup plan safety contract: PASS');
